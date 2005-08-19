package org.ensembl.mart.submitter;
//package.org.ensembl.mart.submitter;


import javax.swing.JInternalFrame;

import java.awt.event.*;
import java.awt.*;

/* Used by InternalFrameDemo.java. */
public class SubmitFrame extends JInternalFrame {
    static int openFrameCount = 0;
    static final int xOffset = 30, yOffset = 30;

    public SubmitFrame() {
        super("Submit",// + (++openFrameCount), 
              true, //resizable
              true, //closable
              true, //maximizable
              true);//iconifiable

        //...Create the GUI and put it in the window...

        //...Then set the window size or call pack...
        setSize(300,300);

        //Set the window's location.
        setLocation(xOffset*openFrameCount, yOffset*openFrameCount);
    }
}