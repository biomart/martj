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

    /** Examines the command line parameters and then passes them to the appropriate front end implementation. */
    public static void main(String[] args) {
        System.out.println("MartExplorer is running.");
        String loggingURL = null;
        String host = null;
        String port = null;
        String user = null;
        String password = null;
        boolean commandline = false;
        Getopt g = new Getopt("MartExplorerApplication", args, "l:ch:P:u:p:");
        int c;
        String arg;
        while ((c = g.getopt()) != -1) {
            switch (c) {
                case 'l':
                    loggingURL = g.getOptarg();
                    break;
                case 'c':
                    commandline = true;
                    break;
                case 'h':
                    host = g.getOptarg();
                    break;
                case 'P':
                    port = g.getOptarg();
                    break;
                case 'u':
                    user = g.getOptarg();
                    break;
                case 'p':
                    password = g.getOptarg();
                    break;
            }
        }
        int offset = g.getOptind();
        int len = args.length - offset;
        String[] cleanArgs = new String[len];
        System.arraycopy(args, offset, cleanArgs, 0, len);
        // Initialise logging system
        if (loggingURL != null) {
            PropertyConfigurator.configure(loggingURL);
        }
        else if (System.getProperty("log4j.configuration") == null) {
            // Initialise logging system to print to logging messages of level >=
            // WARN to console. Don't bother if the system property
            // log4j.configuration is set.
            BasicConfigurator.configure();
            Logger.getRoot().setLevel(Level.WARN);
        }
        Engine engine = new Engine();
        // start interface: if -c then run CommandLineFrontEnd else SwingFrontEnd
        if (commandline)
            new CommandLineFrontEnd(engine, host, port, user, password, cleanArgs).run();
        else
            new MartExplorerGUI().run();
    }

    /** @link dependency */

  /*# CommandLineFrontEnd lnkCommandLineInterface; */

    /** @link dependency */

  /*# SwingFrontEnd lnkSwingInterface; */
}
