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
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.ensembl.mart.guiutils.QuickFrame;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.DSAttributeGroup;

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
  
  private JRadioButton clearButton = new JRadioButton("Clear");

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

  private JRadioButton[] typeButtons =
    new JRadioButton[] { clearButton, transcript, gene };

  private JRadioButton[] includeButtons =
    new JRadioButton[] {
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

  private JRadioButton[] geneButtons =
    new JRadioButton[] {
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

  private ImageIcon transcript3Flank;
  private ImageIcon transcript5Flank;
  private ImageIcon transcriptCdna;
  private ImageIcon transcriptCoding;
  private ImageIcon transcriptPeptide;
  private ImageIcon transcript3UTRPlusFlank;
  private ImageIcon transcript3UTR;
  private ImageIcon transcriptExonsPlus3Flanks;
  private ImageIcon transcriptExonsPlus5And3Flanks;
  private ImageIcon transcriptExonsPlus5Flanks;
  private ImageIcon transcriptExons;
  private ImageIcon transcriptExonsPlusIntronsPlus3Flank;
  private ImageIcon transcriptExonsPlusIntronsPlus5And3Flanks;
  private ImageIcon transcriptExonsPlusIntronsPlus5Flank;
  private ImageIcon transcriptExonsAndIntrons;
  private ImageIcon transcriptEverything;
  private ImageIcon transcript5UTRPlusFlank;
  private ImageIcon transcript5UTR;

  private ImageIcon gene3Flank;
  private ImageIcon gene5Flank;
  private ImageIcon geneExonsPlus3Flanks;
  private ImageIcon geneExonsPlus5And3Flanks;
  private ImageIcon geneExonsPlus5Flanks;
  private ImageIcon geneExons;
  private ImageIcon geneExonsPlusIntronsPlus3Flank;
  private ImageIcon geneExonsPlusIntronsPlusFlanks;
  private ImageIcon geneExonsPlusIntronsPlus5Flank;
  //private ImageIcon geneExons;
  private ImageIcon geneExonsPlusIntrons;

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

    Box b = Box.createVerticalBox();

    b.add(schematicSequenceImageHolder);

    ButtonGroup bg = new ButtonGroup();
    for (int i = 0; i < typeButtons.length; i++) {

      bg.add(typeButtons[i]);
      b.add(typeButtons[i]);
      typeButtons[i].addActionListener(this);

    }

    bg = new ButtonGroup();
    for (int i = 0; i < includeButtons.length; i++) {

      bg.add(includeButtons[i]);
      b.add(includeButtons[i]);
      includeButtons[i].addActionListener(this);

    }

    Box f5 = Box.createHorizontalBox();
    f5.add(new JLabel("5' Flank (bp)"));
    f5.add(flank5);
    b.add(f5);
    
    Box f3 = Box.createHorizontalBox();
    f3.add(new JLabel("3' Flank (bp)"));
    f3.add(flank3);
    b.add(f3);
    
    // TODO listen to changes in the flank text fields
    
    add(b);

    disableButtons();
  }

  private void loadSchematicSequenceImages() {

    transcript3Flank = loadIcon("data/image/gene_schematic_3_only.gif");
    transcript5Flank = loadIcon("data/image/gene_schematic_5_only.gif");
    transcriptCdna = loadIcon("data/image/gene_schematic_cdna.gif");
    transcriptCoding = loadIcon("data/image/gene_schematic_coding.gif");
    transcriptPeptide =
      loadIcon("data/image/gene_schematic_coding_translation.gif");
    transcript3UTRPlusFlank =
      loadIcon("data/image/gene_schematic_downstream_utr_3.gif");
    transcript3UTR = loadIcon("data/image/gene_schematic_downstream_utr.gif");
    transcriptExonsPlus3Flanks =
      loadIcon("data/image/gene_schematic_exons_3.gif");
    transcriptExonsPlus5And3Flanks =
      loadIcon("data/image/gene_schematic_exons_5_3.gif");
    transcriptExonsPlus5Flanks =
      loadIcon("data/image/gene_schematic_exons_5.gif");
    transcriptExons = loadIcon("data/image/gene_schematic_exons.gif");

    transcriptExonsPlusIntronsPlus3Flank =
      loadIcon("data/image/gene_schematic_gene_3.gif");
    transcriptExonsPlusIntronsPlus5And3Flanks =
      loadIcon("data/image/gene_schematic_gene_5_3.gif");
    transcriptExonsPlusIntronsPlus5Flank =
      loadIcon("data/image/gene_schematic_gene_5.gif");
    transcriptExonsAndIntrons =
      loadIcon("data/image/gene_schematic_gene_only.gif");
    transcriptEverything = loadIcon("data/image/gene_schematic.gif");
    transcript5UTRPlusFlank =
      loadIcon("data/image/gene_schematic_upstream_utr_5.gif");
    transcript5UTR = loadIcon("data/image/gene_schematic_upstream_utr.gif");

    gene3Flank = loadIcon("data/image/gene_schematic_extent_3_only.gif");
    gene5Flank = loadIcon("data/image/gene_schematic_extent_5_only.gif");
    geneExonsPlus3Flanks =
      loadIcon("data/image/gene_schematic_extent_exons_3.gif");
    geneExonsPlus5And3Flanks =
      loadIcon("data/image/gene_schematic_extent_exons_5_3.gif");
    geneExonsPlus5Flanks =
      loadIcon("data/image/gene_schematic_extent_exons_5.gif");
    geneExons = loadIcon("data/image/gene_schematic_extent_exons.gif");
    geneExonsPlusIntronsPlus3Flank =
      loadIcon("data/image/gene_schematic_extent_gene_3.gif");
    geneExonsPlusIntronsPlusFlanks =
      loadIcon("data/image/gene_schematic_extent_gene_5_3.gif");
    geneExonsPlusIntronsPlus5Flank =
      loadIcon("data/image/gene_schematic_extent_gene_5.gif");
    geneExonsPlusIntrons =
      loadIcon("data/image/gene_schematic_extent_gene_only.gif");
    // both same as above?
    //geneExons = loadIcon("data/image/gene_schematic_extent_gene_exons.gif");
    //geneExons = loadIcon("data/image/gene_schematic_gene_exons.gif");

    // create the blankIcon
    int w = transcript3Flank.getIconWidth();
    int h = transcript3Flank.getIconHeight();
    BufferedImage blank =
      new BufferedImage(
        w,
        h,
        BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = blank.createGraphics();
    g.setBackground(Color.WHITE);
    g.fillRect(0,0,w,h);
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
    System.out.println("button clicked");

    Object src = e.getSource();

    if (clearButton.isSelected() && src == clearButton) {

      disableButtons();

      // remove filter if necessary
      schematicSequenceImageHolder.setIcon(blankIcon);

    } else if (transcript.isSelected()) {

      if (src == transcript) {
        disableButtons();
        enableTranscriptButtons();
      } else if (includeDownStreamUTROnly.isSelected()) {
        schematicSequenceImageHolder.setIcon(transcript3Flank);
      } else if (includeUpStreamUTROnly.isSelected()) {
        schematicSequenceImageHolder.setIcon(transcript5Flank);
      }

    } else if (gene.isSelected()) {

      if (src == gene) {
        disableButtons();
        enableGeneButtons();
      }

      //  TODO enable gene only options if src==gene

      // TODO gene options.

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

  private void disableButtons() {
    for (int i = 0; i < includeButtons.length; i++)
      includeButtons[i].setEnabled(false);
  }

  // TODO remove / add / change filter if necessary
}
