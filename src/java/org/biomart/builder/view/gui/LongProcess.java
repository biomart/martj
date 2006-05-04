/*
 * LongProcess.java
 *
 * Created on 03 May 2006, 16:13
 */

/*
        Copyright (C) 2006 EBI
 
        This library is free software; you can redistribute it and/or
        modify it under the terms of the GNU Lesser General Public
        License as published by the Free Software Foundation; either
        version 2.1 of the License, or (at your option) any later version.
 
        This library is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the itmplied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
        Lesser General Public License for more details.
 
        You should have received a copy of the GNU Lesser General Public
        License along with this library; if not, write to the Free Software
        Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.biomart.builder.view.gui;

import java.awt.Container;
import java.awt.Cursor;

/**
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 */
public abstract class LongProcess {
    
    private static int longProcessCount = 0;
    
    private static Object lockObject = new Object();
    
    /** Creates a new instance of LongProcess */
    private LongProcess() {}
    
    public static void run(final Container hourglassParent, final Runnable process) {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    synchronized (lockObject) {
                        LongProcess.longProcessCount++;
                    }
                    if (LongProcess.longProcessCount == 1) {
                        // Start hourglass
                        Cursor hourglassCursor = new Cursor(Cursor.WAIT_CURSOR);
                        hourglassParent.setCursor(hourglassCursor);
                    }
                    process.run();
                } catch (Error e) {
                    throw e;
                } finally {
                    synchronized (lockObject) {
                        LongProcess.longProcessCount--;
                    }
                    if (LongProcess.longProcessCount == 0) {
                        // Stop hourglass.
                        Cursor normalCursor = new Cursor(Cursor.DEFAULT_CURSOR);
                        hourglassParent.setCursor(normalCursor);
                    }
                }
            }
        });
        t.start();
    }
}
