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

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.RepaintManager;
import javax.swing.filechooser.FileFilter;

import org.biomart.builder.resources.Resources;
import org.biomart.builder.view.gui.MartTabSet.MartTab;

/**
 * Saves any given component to an image file.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 25th July 2006
 * @since 0.1
 */
public class ComponentImageSaver {

	private MartTab martTab;

	private Component component;

	/**
	 * Constructs a component saver that is associated with the given mart tab.
	 * 
	 * @param martTab
	 *            the mart tab to associate it with.
	 * @param component
	 *            the component to save.
	 */
	public ComponentImageSaver(final MartTab martTab, final Component component) {
		this.component = component;
		this.martTab = martTab;
	}

	/**
	 * Pops up a save-as dialog, and if the user completes it correctly, saves
	 * the component.
	 */
	public void save() {
		// Popup a save as dialog to request filename and image format.
		final JFileChooser fileChooser = new JFileChooser();
		fileChooser.setAcceptAllFileFilterUsed(false);
		fileChooser.addChoosableFileFilter(new ImageSaverFilter("png",
				Resources.get("PNGFileFilterDescription"),
				new String[] { ".png" }));
		fileChooser.addChoosableFileFilter(new ImageSaverFilter("jpeg",
				Resources.get("JPEGFileFilterDescription"), new String[] {
						".jpg", "*.jpeg" }));
		if (fileChooser.showSaveDialog(this.martTab) == JFileChooser.APPROVE_OPTION)
			// Call save() with the filename and format.
			LongProcess.run(new Runnable() {
				public void run() {
					try {
						ComponentImageSaver.this.save(fileChooser
								.getSelectedFile(),
								(ImageSaverFilter) fileChooser.getFileFilter());
					} catch (final IOException e) {
						ComponentImageSaver.this.martTab.getMartTabSet()
								.getMartBuilder().showStackTrace(e);
					}
				}
			});
	}

	private void save(final File file, final ImageSaverFilter format)
			throws IOException {
		// Create an image the same size as the component.
		final BufferedImage image = GraphicsEnvironment
				.getLocalGraphicsEnvironment().getDefaultScreenDevice()
				.getDefaultConfiguration().createCompatibleImage(
						this.component.getWidth(), this.component.getHeight());
		// Render the component onto the image.
		final Graphics2D g2d = image.createGraphics();
		final RepaintManager currentManager = RepaintManager
				.currentManager(this.component);
		currentManager.setDoubleBufferingEnabled(false);
		this.component.paintAll(g2d);
		currentManager.setDoubleBufferingEnabled(true);
		// Save the image in the given format to the given filename.
		ImageIO.write(image, format.getFormat(), file);
	}

	/**
	 * This class represents a filter for a particular image format.
	 */
	private class ImageSaverFilter extends FileFilter {

		private String format;

		private String description;

		private String[] extensions;

		/**
		 * The constructor accepts a format name, a description, and a list of
		 * matching extensions.
		 * 
		 * @param format
		 *            the format name.
		 * @param description
		 *            the description to put in the file chooser.
		 * @param extensions
		 *            the list of matching extensions.
		 */
		public ImageSaverFilter(final String format, final String description,
				final String[] extensions) {
			this.format = format;
			this.description = description;
			this.extensions = extensions;
		}

		/**
		 * Find out what image format this filter represents.
		 * 
		 * @return the format this filter represents.
		 */
		public String getFormat() {
			return this.format;
		}

		public boolean accept(final File f) {
			if (f.isDirectory())
				return true;
			else {
				final String name = f.getName().toLowerCase();
				for (int i = 0; i < this.extensions.length; i++)
					if (name.endsWith(this.extensions[i]))
						return true;
			}
			return false;
		}

		public String getDescription() {
			return this.description;
		}
	}
}
