package org.ensembl.mart.submitter;

import javax.swing.JInternalFrame;

import java.awt.event.*;
import java.awt.*;

/**
 * Class SubmitFrame extends JInternalFrame..
 * 
 * @author <a href="mailto:kasprzo3@man.ac.uk">Olga Kasprzyk</a> 
 *         
 */

public class SubmitFrame extends JInternalFrame {
    static int openFrameCount = 0;
    static final int xOffset = 30, yOffset = 30;

    public SubmitFrame() {
        super("Submit", 
              true, //resizable
              true, //closable
              true, //maximizable
              true);//iconifiable

        //Set the window size
        setSize(300,300);

        //Set the window's location.
        setLocation(xOffset*openFrameCount, yOffset*openFrameCount);
    }
}