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

import org.apache.log4j.*;
import gnu.getopt.*;
import org.ensembl.mart.explorer.gui.*;

/**
 * The MartExplorer Application enables end users to construct queries
 * against Mart databases and export the results in a variety of formats.
 */
public class MartExplorerApplication {
    public MartExplorerApplication() {
    }


  /**
   * Initialise logging system to print to logging messages of level >= WARN
   * to console. Does nothing if system property log4j.configuration is set.
   */
  public static void defaultLoggingConfiguration() {
    if (System.getProperty("log4j.configuration") == null) {
      
      BasicConfigurator.configure();
      Logger.getRoot().setLevel(Level.WARN);
    }
  }

    /** Examines the command line parameters and then passes them to the appropriate front end implementation. */
    public static void main(String[] args) {
        String loggingURL = null;
        boolean commandline = false;
        boolean help = false;
        Getopt g = new Getopt("MartExplorerApplication", args, CommandLineFrontEnd.COMMAND_LINE_SWITCHES);
        int c;
        while ((c = g.getopt()) != -1) {
          switch (c) {
          case 'l':
            loggingURL = g.getOptarg();
            break;
          case 'H':
            // if host specified assume command line mode
            commandline = true;
            break;

          case 'h':
            help = true;
            break;
          }
        }
        int offset = g.getOptind();
        int len = args.length - offset;
        // Initialise logging system
        if (loggingURL != null) {
            PropertyConfigurator.configure(loggingURL);
        }
        else {
          defaultLoggingConfiguration();
        }


        if ( help ) {
          System.out.println( CommandLineFrontEnd.usage() );
        }

        else if (commandline) {
          CommandLineFrontEnd cfe = new CommandLineFrontEnd();
          cfe.init( args );
          cfe.run();
        }
        else {
          new MartExplorerGUI().run();
        }
    }

    /** @link dependency */

  /*# CommandLineFrontEnd lnkCommandLineInterface; */

    /** @link dependency */

  /*# SwingFrontEnd lnkSwingInterface; */
}
