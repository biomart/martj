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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.ensembl.mart.guiutils.QuickFrame;
import org.ensembl.mart.lib.InvalidQueryException;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.SequenceDescription;
import org.ensembl.mart.lib.config.DSAttributeGroup;
import org.ensembl.mart.util.*;

/**
 * Widget for selecting sequence information for inclusion in query.
 * 
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 *
 * TODO test that button -> correct filters being added to query
 * TODO add support for other sequence types
  * TODO selecting button -> add/change filter on query
  * 
 * TODO listen to changes in query e.g. deleted filter.
 */
public class SequenceGroupWidget
  extends GroupWidget
  implements ActionListener {

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

  private SequenceDescription oldAttribute = null;

  private Feedback feedback = new Feedback(this);

  private DSAttributeGroup attributeGroup;

  private LabelledTextField flank5 = new LabelledTextField("1000");
  private LabelledTextField flank3 = new LabelledTextField("1000");

  private JRadioButton clearButton = new JRadioButton("None");

  private JRadioButton transcript = new JRadioButton("Transcripts/proteins");

  private JRadioButton gene =
    new JRadioButton("Genes - transcript information ignored (one output per gene)");

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

  private JRadioButton[] typeButtons = { transcript, gene, clearButton };

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

    this.attributeGroup = attributeGroup;

    loadSchematicSequenceImages();
    buildGUI();
    configureWidgets();
    reset();
  }

  private void configureWidgets() {

    clearButton.setSelected(true);
    ButtonGroup bg = new ButtonGroup();
    for (int i = 0; i < typeButtons.length; i++) {
      bg.add(typeButtons[i]);
      typeButtons[i].addActionListener(this);
    }

    bg = new ButtonGroup();
    for (int i = 0; i < includeButtons.length; i++) {
      bg.add(includeButtons[i]);
      includeButtons[i].addActionListener(this);
    }
    bg.add(includeNone);

    flank5.addActionListener(this);
    flank3.addActionListener(this);

  }

  private void buildGUI() {

    Box b = Box.createVerticalBox();

    b.add(addAll(Box.createHorizontalBox(), typeButtons, true));

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

  private void loadSchematicSequenceImages() {

  }

  private ImageIcon loadIcon(String filepath) {
    ImageIcon icon = null;
    try {
      BufferedImage testImage = ImageIO.read(new File(filepath));
      icon = new ImageIcon(testImage);
    } catch (IOException e) {
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

    DSAttributeGroup g = new DSAttributeGroup("sequences");
    Query q = new Query();
    q.addQueryChangeListener(new DebugQueryListener(System.out));

    SequenceGroupWidget w = new SequenceGroupWidget("seq widget", q, null, g);

    new QuickFrame("Sequence Attribute Widget test", w);
  }

  /**
   * Handles all user clicks on buttons in the widget. Causes the preview to update
   * and adds / removes a filtter to/from query as necessary.
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  public void actionPerformed(ActionEvent e) {

    Object src = e.getSource();

    disableFlanks();

    if (src == clearButton) {

      // a bit of hack; use this image because the method requires one. It will
      // be replaced in the call to reset.
      updateState("data/image/gene_schematic_gene_only.gif", NONE, 0,0);
      reset();

    } else if (transcript.isSelected()) {

      if (src == transcript) {

        reset();
        enableTranscriptButtons();
      
      } else if (includeGeneSequence.isSelected()) {

        updateState(
        "data/image/gene_schematic_gene_only.gif",
          SequenceDescription.GENEEXONINTRON,
          0,
          0);

      } else if (includeGeneSequence_5_3.isSelected()) {

        updateState(
          "data/image/gene_schematic_gene_5_3.gif",
          SequenceDescription.TRANSCRIPTEXONINTRON,
          flank5.getTextAsInt(),
          flank3.getTextAsInt());
        flank5.setEnabled(true);
        flank3.setEnabled(true);

      } else if (includeGeneSequence_5.isSelected()) {

        updateState(
          "data/image/gene_schematic_gene_5.gif",
          SequenceDescription.TRANSCRIPTFLANKS,
          flank5.getTextAsInt(),
          0);
        flank5.setEnabled(true);

      } else if (includeUpstream.isSelected()) {

        //      TODO transcript upstream sequence support
        updateState(
          "data/image/gene_schematic_5_only.gif",
          UNSUPPORTED,
          flank5.getTextAsInt(),
          0);
        flank5.setEnabled(true);

      } else if (includeUpStreamUTROnly.isSelected()) {

        updateState(
          "data/image/gene_schematic_upstream_utr.gif",
          SequenceDescription.UPSTREAMUTR,
          0,
          0);

      } else if (includeUpStreamAndUTR.isSelected()) {

        updateState("data/image/gene_schematic_upstream_utr_5.gif", UNSUPPORTED, 0, 0);
        flank5.setEnabled(true);

      } else if (includeGeneSequence_3.isSelected()) {

        updateState(
          "data/image/gene_schematic_gene_3.gif",
          SequenceDescription.TRANSCRIPTFLANKS,
          0,
          flank3.getTextAsInt());
        flank3.setEnabled(true);

      } else if (includeDownStream.isSelected()) {

        // TODO transcript downstream support
        updateState(
          "data/image/gene_schematic_3_only.gif",
          UNSUPPORTED,
          0,
          flank3.getTextAsInt());
        flank3.setEnabled(true);

      } else if (includeDownStreamUTROnly.isSelected()) {

        updateState(
          "data/image/gene_schematic_downstream_utr.gif",
          SequenceDescription.DOWNSTREAMUTR,
          0,
          0);

      } else if (includeDownStreamAndUTR.isSelected()) {

        //      TODO transcript downstream + UTR
        updateState("data/image/gene_schematic_downstream_utr_3.gif", UNSUPPORTED, 0, 0);
        flank3.setEnabled(true);

      } else if (includeExonSequence.isSelected()) {

        updateState(
          "data/image/gene_schematic_exons.gif",
          SequenceDescription.TRANSCRIPTEXONS,
          0,
          0);

      } else if (includecDNASequence.isSelected()) {

        updateState(
          "data/image/gene_schematic_cdna.gif",
          SequenceDescription.TRANSCRIPTCDNA,
          0,
          0);

      } else if (includeCodingSequence.isSelected()) {

        updateState(
          "data/image/gene_schematic_coding.gif",
          SequenceDescription.TRANSCRIPTCODING,
          0,
          0);

      } else if (includePeptide.isSelected()) {

        updateState(
          "data/image/gene_schematic_coding.gif",
          SequenceDescription.TRANSCRIPTPEPTIDE,
          0,
          0);

      } else if (includeExonsPlus5And3Flanks.isSelected()) {

        updateState(
          "data/image/gene_schematic_exons_5_3.gif",
          SequenceDescription.TRANSCRIPTEXONS,
          flank5.getTextAsInt(),
          flank3.getTextAsInt());
        flank5.setEnabled(true);
        flank3.setEnabled(true);

      } else if (includeExonsPlus3Flanks.isSelected()) {

        updateState(
          "data/image/gene_schematic_exons_3.gif",
          SequenceDescription.TRANSCRIPTEXONS,
          0,
          flank3.getTextAsInt());
        flank3.setEnabled(true);

      } else if (includeExonsPlus5Flanks.isSelected()) {

        updateState(
          "data/image/gene_schematic_exons_5.gif",
          SequenceDescription.TRANSCRIPTEXONS,
          flank5.getTextAsInt(),
          0);
        flank5.setEnabled(true);

      }

    } else if (gene.isSelected()) {

      if (src == gene) {

        reset();
        enableGeneButtons();

      } else if (includeGeneSequence.isSelected()) {

        updateState(
          "data/image/gene_schematic_extent_gene_only.gif",
          SequenceDescription.GENEEXONINTRON,
          0,
          0);

      } else if (includeGeneSequence_5_3.isSelected()) {

        updateState(
          "data/image/gene_schematic_extent_gene_5_3.gif",
          SequenceDescription.GENEEXONINTRON,
          flank5.getTextAsInt(),
          flank3.getTextAsInt());

        flank5.setEnabled(true);
        flank3.setEnabled(true);

      } else if (includeGeneSequence_5.isSelected()) {

        updateState(
          "data/image/gene_schematic_extent_gene_5.gif",
          SequenceDescription.GENEFLANKS,
          flank5.getTextAsInt(),
          0);
        flank5.setEnabled(true);

      } else if (includeGeneSequence_3.isSelected()) {

        updateState(
          "data/image/gene_schematic_extent_gene_3.gif",
          SequenceDescription.GENEFLANKS,
          0,
          flank3.getTextAsInt());

        flank3.setEnabled(true);

      } else if (includeUpstream.isSelected()) {

        updateState(
          "data/image/gene_schematic_extent_5_only.gif",
          UNSUPPORTED,
          flank5.getTextAsInt(),
          0);
        flank5.setEnabled(true);

      } else if (includeDownStream.isSelected()) {

        updateState(
          "data/image/gene_schematic_extent_3_only.gif",
          UNSUPPORTED,
          0,
          flank3.getTextAsInt());
        flank3.setEnabled(true);

      } else if (includeExonSequence.isSelected()) {

        updateState(
          "data/image/gene_schematic_extent_exons.gif",
          SequenceDescription.GENEEXONS,
          0,
          0);

      } else if (includeExonsPlus5And3Flanks.isSelected()) {

        updateState(
          "data/image/gene_schematic_extent_exons_5_3.gif",
          SequenceDescription.GENEEXONS,
          flank5.getTextAsInt(),
          flank3.getTextAsInt());

        flank5.setEnabled(true);
        flank3.setEnabled(true);

      } else if (includeExonsPlus3Flanks.isSelected()) {

        updateState(
          "data/image/gene_schematic_extent_exons_3.gif",
          SequenceDescription.GENEEXONS,
          0,
          flank3.getTextAsInt());

        flank3.setEnabled(true);

      } else if (includeExonsPlus5Flanks.isSelected()) {

        updateState(
          "data/image/gene_schematic_extent_exons_5.gif",
          SequenceDescription.GENEEXONS,
          flank5.getTextAsInt(),
          0);

        flank5.setEnabled(true);

      }
    }

  }

  private void disableFlanks() {
    flank5.setEnabled(false);
    flank3.setEnabled(false);
  }

  /**
   * Removes filter if set and disables buttons.
   *
   */
  private void reset() {
    includeNone.setSelected(true);
    disableButtons();
    schematicSequenceImageHolder.setIcon(blankIcon);
    disableFlanks();
  }

  /**
   * Updates the displayed image and adds a filter to the query if appropriatte.
   * @param imageFilePath image to be displayed
   * @param sequenceType sequence type (constant from SeequenceDescription), or UNSUPPORTED if unsupported
   * @param leftFlank left flank in base pairs
   * @param rightFlank rightt flank in base pairs
   */
  private void updateState(
    String imageFilePath,
    int sequenceType,
    int leftFlank,
    int rightFlank) {
    schematicSequenceImageHolder.setIcon(loadIcon(imageFilePath));

    if (sequenceType == -1) {
      feedback.warning("Unsupported sequence type.");
      return;
    }

    SequenceDescription newAttribute = null;

    if (sequenceType != NONE) {

      try {
        newAttribute =
          new SequenceDescription(sequenceType, leftFlank, rightFlank);
      } catch (InvalidQueryException e) {
        feedback.warning("Invalid sequence attribute. " + e.getMessage());
        e.printStackTrace();
        newAttribute = null;
      }
    }

    query.setSequenceDescription(newAttribute);

    oldAttribute = newAttribute;

  }

  private void enableGeneButtons() {
    for (int i = 0; i < geneButtons.length; i++)
      geneButtons[i].setEnabled(true);

  }

  private void enableTranscriptButtons() {
    for (int i = 0; i < includeButtons.length; i++)
      includeButtons[i].setEnabled(true);
  }

  private void disableButtons() {
    for (int i = 0; i < includeButtons.length; i++)
      includeButtons[i].setEnabled(false);
  }

}
