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

package org.biomart.common.view.gui;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import javax.swing.RepaintManager;


/**
 * Prints any given component.
 * <p>
 * Based on code from <a
 * href="http://www.apl.jhu.edu/~hall/java/Swing-Tutorial/Swing-Tutorial-Printing.html">this
 * Swing Tutorial</a>.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by 
 * 			$Author$
 * @since 0.1
 */
public class ComponentPrinter implements Printable {

	private Component component;

	/**
	 * Constructs a component printer that is associated with the given mart
	 * tab.
	 * 
	 * @param component
	 *            the component to print.
	 */
	public ComponentPrinter(final Component component) {
		this.component = component;
	}

	/**
	 * Pops up a printing dialog, and if the user completes it correctly, prints
	 * the component.
	 */
	public void print() {
		final PrinterJob printJob = PrinterJob.getPrinterJob();
		printJob.setPrintable(this);
		if (printJob.printDialog())
			LongProcess.run(new Runnable() {
				public void run() {
					try {
						printJob.print();
					} catch (final PrinterException pe) {
						StackTrace.showStackTrace(pe);
					}
				}
			});
	}

	public int print(final Graphics g, final PageFormat pageFormat,
			final int pageIndex) {
		// Work out the printable area.
		final Rectangle2D printableArea = new Rectangle2D.Double(pageFormat
				.getImageableX(), pageFormat.getImageableY(), pageFormat
				.getImageableWidth(), pageFormat.getImageableHeight());
		// Simple scale to reduce component size.
		final double xscale = 0.5;
		final double yscale = 0.5;
		// Work out pages required for the component we are drawing.
		final int pagesAcross = (int) Math.ceil(this.component.getWidth()
				* xscale / printableArea.getWidth());
		final int pagesDown = (int) Math.ceil(this.component.getHeight()
				* yscale / printableArea.getHeight());
		final int numPages = pagesAcross * pagesDown;
		// If we are beyond the last page, we are done.
		if (pageIndex >= numPages)
			return Printable.NO_SUCH_PAGE;
		else {
			// Print the components.
			final Graphics2D g2d = (Graphics2D) g;
			// Translate our output to the printable area.
			g2d.translate(printableArea.getX(), printableArea.getY());
			// What page are we being asked to print?
			int pageXNum = pageIndex;
			while (pageXNum >= pagesAcross)
				pageXNum -= pagesAcross;
			int pageYNum = pageIndex - pageXNum;
			while (pageYNum >= pagesDown)
				pageYNum -= pagesDown;
			// Translate our output to focus on the required page.
			g2d.translate(-printableArea.getWidth() * pageXNum * xscale,
					-printableArea.getHeight() * pageYNum * yscale);
			// Scale our output down a bit as otherwise the objects are
			// huge on paper.
			g2d.scale(xscale, yscale);
			// Do the printing.
			final RepaintManager currentManager = RepaintManager
					.currentManager(this.component);
			currentManager.setDoubleBufferingEnabled(false);
			this.component.printAll(g2d);
			currentManager.setDoubleBufferingEnabled(true);
			return Printable.PAGE_EXISTS;
		}
	}
}
