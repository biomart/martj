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
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.DSAttributeGroup;

import sun.awt.HorizBagLayout;

/**
 * Widget for selecting sequence attributes.
 * 
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 *
 * TODO selecting button -> change preview image
 * TODO layout component
 * TODO selecting button -> add/change filter on query
 * TODO enable / disable flank boxes
 * TODO listen to flank changes
 * 
 * TODO listen to changes in query e.g. deleted filter.
 */
public class SequenceGroupWidget
  extends GroupWidget
  implements ActionListener {

  private DSAttributeGroup attributeGroup;

  private JTextField flank5 = new JTextField("1000");
  private JTextField flank3 = new JTextField("1000");

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

    clearButton.setSelected(true);

    loadSchematicSequenceImages();

    Box b = Box.createVerticalBox();

    Box types = Box.createHorizontalBox();
    ButtonGroup bg = new ButtonGroup();
    for (int i = 0; i < typeButtons.length; i++) {

      bg.add(typeButtons[i]);
      types.add(typeButtons[i]);
      typeButtons[i].addActionListener(this);

    }
    b.add(types);
    
    Box schemaBox = Box.createHorizontalBox();
    schemaBox.add(schematicSequenceImageHolder);
    schemaBox.add(Box.createHorizontalGlue());
    b.add(schemaBox);

    bg = new ButtonGroup();
    for (int i = 0; i < includeButtons.length; i++) {

      bg.add(includeButtons[i]);
      includeButtons[i].addActionListener(this);

    }
    bg.add(includeNone);

    Box columns = Box.createHorizontalBox();
    Box left = Box.createVerticalBox();
    for (int i = 0; i < leftColumn.length; i++) {
      left.add(leftColumn[i]);
    }
    columns.add(left);

    Box right = Box.createVerticalBox();
    for (int i = 0; i < rightColumn.length; i++) {
      right.add(rightColumn[i]);
    }
    right.add(Box.createVerticalGlue());
    columns.add(right);
    columns.add(Box.createHorizontalGlue());

    b.add(columns);

    Box flanks = Box.createHorizontalBox();
    Dimension d = new Dimension(100, 24);
    flank5.setPreferredSize(d);
    flank5.setMaximumSize(d);
    flanks.add(new JLabel("5' Flank (bp)"));
    flanks.add(flank5);

    flanks.add(Box.createHorizontalStrut(50));

    flank3.setPreferredSize(d);
    flank3.setMaximumSize(d);
    flanks.add(new JLabel("3' Flank (bp)"));
    flanks.add(flank3);

    b.add(flanks);

    // TODO listen to changes in the flank text fields

    add(b);

    reset();
  }

  private void loadSchematicSequenceImages() {

    ImageIcon transcript3Flank =
      loadIcon("data/image/gene_schematic_3_only.gif");

    // create the blankIcon
    int w = transcript3Flank.getIconWidth();
    int h = transcript3Flank.getIconHeight();
    BufferedImage blank = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = blank.createGraphics();
    g.setBackground(Color.WHITE);
    g.fillRect(0, 0, w, h);
    blankIcon = new ImageIcon(blank);
    schematicSequenceImageHolder.setIcon(blankIcon);

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

      reset();

    } else if (transcript.isSelected()) {

      if (src == transcript) {

        reset();
        enableTranscriptButtons();

      } else if (includeGeneSequence.isSelected()) {

        updateState("data/image/gene_schematic_gene_only.gif");

      } else if (includeGeneSequence_5_3.isSelected()) {

        updateState("data/image/gene_schematic_gene_5_3.gif");
        flank5.setEnabled(true);
        flank3.setEnabled(true);

      } else if (includeGeneSequence_5.isSelected()) {

        updateState("data/image/gene_schematic_gene_5.gif");
        flank5.setEnabled(true);

      } else if (includeUpstream.isSelected()) {

        updateState("data/image/gene_schematic_5_only.gif");
        flank5.setEnabled(true);

      } else if (includeUpStreamUTROnly.isSelected()) {

        updateState("data/image/gene_schematic_upstream_utr.gif");

      } else if (includeUpStreamAndUTR.isSelected()) {

        updateState("data/image/gene_schematic_upstream_utr_5.gif");
        flank5.setEnabled(true);

      } else if (includeGeneSequence_3.isSelected()) {

        updateState("data/image/gene_schematic_gene_3.gif");
        flank3.setEnabled(true);

      } else if (includeDownStream.isSelected()) {

        updateState("data/image/gene_schematic_3_only.gif");
        flank3.setEnabled(true);

      } else if (includeDownStreamUTROnly.isSelected()) {

        updateState("data/image/gene_schematic_downstream_utr.gif");

      } else if (includeDownStreamAndUTR.isSelected()) {

        updateState("data/image/gene_schematic_downstream_utr_3.gif");
        flank3.setEnabled(true);

      } else if (includeExonSequence.isSelected()) {

        updateState("data/image/gene_schematic_exons.gif");

      } else if (includecDNASequence.isSelected()) {

        updateState("data/image/gene_schematic_cdna.gif");

      } else if (includeCodingSequence.isSelected()) {

        updateState("data/image/gene_schematic_coding.gif");

      } else if (includePeptide.isSelected()) {

        updateState("data/image/gene_schematic_coding.gif");

      } else if (includeExonsPlus5And3Flanks.isSelected()) {

        updateState("data/image/gene_schematic_exons_5_3.gif");
        flank5.setEnabled(true);
        flank3.setEnabled(true);

      } else if (includeExonsPlus3Flanks.isSelected()) {

        updateState("data/image/gene_schematic_exons_3.gif");
        flank3.setEnabled(true);
        
      } else if (includeExonsPlus5Flanks.isSelected()) {

        updateState("data/image/gene_schematic_exons_5.gif");
        flank5.setEnabled(true);
        
      }

    } else if (gene.isSelected()) {

      if (src == gene) {

        reset();
        enableGeneButtons();

      } else if (includeGeneSequence.isSelected()) {

        updateState("data/image/gene_schematic_extent_gene_only.gif");

      } else if (includeGeneSequence_5_3.isSelected()) {

        updateState("data/image/gene_schematic_extent_gene_5_3.gif");
        flank5.setEnabled(true);
        flank3.setEnabled(true);
      
      } else if (includeGeneSequence_5.isSelected()) {

        updateState("data/image/gene_schematic_extent_gene_5.gif");
        flank5.setEnabled(true);
        
      } else if (includeGeneSequence_3.isSelected()) {

        updateState("data/image/gene_schematic_extent_gene_3.gif");
        flank3.setEnabled(true);
        
      } else if (includeUpstream.isSelected()) {

        updateState("data/image/gene_schematic_extent_5_only.gif");
        flank5.setEnabled(true);

      } else if (includeDownStream.isSelected()) {

        updateState("data/image/gene_schematic_extent_3_only.gif");
        flank3.setEnabled(true);

      } else if (includeExonSequence.isSelected()) {

        updateState("data/image/gene_schematic_extent_exons.gif");

      } else if (includeExonsPlus5And3Flanks.isSelected()) {

        updateState("data/image/gene_schematic_extent_exons_5_3.gif");
        flank5.setEnabled(true);
        flank3.setEnabled(true);

      } else if (includeExonsPlus3Flanks.isSelected()) {

        updateState("data/image/gene_schematic_extent_exons_3.gif");
        flank3.setEnabled(true);

      } else if (includeExonsPlus5Flanks.isSelected()) {

        updateState("data/image/gene_schematic_extent_exons_5.gif");
        flank5.setEnabled(true);

      }

      //  TODO enable gene only options if src==gene

      // TODO gene options.

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
    // TODO remove filter if necessary
  }

  private void updateState(String imageFilePath) {
    schematicSequenceImageHolder.setIcon(loadIcon(imageFilePath));
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

  // TODO remove / add / change filter if necessary
}
