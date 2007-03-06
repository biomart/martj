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

package org.biomart.builder.view.gui.diagrams.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceContext;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.view.gui.diagrams.Diagram;
import org.biomart.common.model.Column;
import org.biomart.common.model.Key;
import org.biomart.common.model.Key.PrimaryKey;
import org.biomart.common.view.gui.StackTrace;


/**
 * Represents a key by listing out in a set of labels each column in the key.
 * <p>
 * Drag-and-drop code courtesy of <a
 * href="http://www.javaworld.com/javaworld/jw-03-1999/jw-03-dragndrop.html?page=1">JavaWorld</a>.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.1
 */
public class KeyComponent extends BoxShapedComponent {
	private static final long serialVersionUID = 1;

	/**
	 * Constant referring to foreign key colour.
	 */
	public static Color FK_BACKGROUND_COLOUR = Color.YELLOW;

	/**
	 * Constant referring to handmade key colour.
	 */
	public static Color HANDMADE_COLOUR = Color.GREEN;

	/**
	 * Constant referring to incorrect key colour.
	 */
	public static Color INCORRECT_COLOUR = Color.RED;

	/**
	 * Constant referring to masked key colour.
	 */
	public static Color MASKED_COLOUR = Color.LIGHT_GRAY;

	/**
	 * Constant referring to normal key colour.
	 */
	public static Color NORMAL_COLOUR = Color.DARK_GRAY;

	/**
	 * Constant referring to primary key colour.
	 */
	public static Color PK_BACKGROUND_COLOUR = Color.CYAN;

	/**
	 * Plain font.
	 */
	public static Font PLAIN_FONT = Font.decode("SansSerif-PLAIN-10");

	private GridBagConstraints constraints;

	private boolean draggable;

	private GridBagLayout layout;

	/**
	 * The constructor constructs a key component around a given key object, and
	 * associates it with the given display.
	 * 
	 * @param key
	 *            the key to represent.
	 * @param diagram
	 *            the diagram to draw the key on.
	 */
	public KeyComponent(final Key key, final Diagram diagram) {
		super(key, diagram);

		// Key components are set out in a vertical list.
		this.layout = new GridBagLayout();
		this.setLayout(this.layout);
		this.draggable = false;

		// Constraints for each column in the key.
		this.constraints = new GridBagConstraints();
		this.constraints.gridwidth = GridBagConstraints.REMAINDER;
		this.constraints.fill = GridBagConstraints.HORIZONTAL;
		this.constraints.anchor = GridBagConstraints.CENTER;
		this.constraints.insets = new Insets(0, 1, 0, 2);

		// Calculate the component layout.
		this.recalculateDiagramComponent();

		// Mark ourselves as handling 'draggedKey' events, for
		// drag-and-drop capabilities.		
		final DragSource dragSource = DragSource.getDefaultDragSource();
		final DragSourceListener dsListener = new DragSourceListener() {
			public void dragEnter(DragSourceDragEvent e) {
				DragSourceContext context = e.getDragSourceContext();
				int myaction = e.getDropAction();
				if ((myaction & DnDConstants.ACTION_COPY) != 0) {
					context.setCursor(DragSource.DefaultLinkDrop);
				} else {
					context.setCursor(DragSource.DefaultLinkNoDrop);
				}
			}

			public void dragDropEnd(DragSourceDropEvent e) {
			}

			public void dragExit(DragSourceEvent dse) {
				DragSourceContext context = dse.getDragSourceContext();
				context.setCursor(DragSource.DefaultLinkNoDrop);
			}

			public void dragOver(DragSourceDragEvent dsde) {
				this.dragEnter(dsde);
			}

			public void dropActionChanged(DragSourceDragEvent dsde) {
				this.dragEnter(dsde);
			}
		};
		final DragGestureListener dgListener = new DragGestureListener() {
			public void dragGestureRecognized(DragGestureEvent e) {
				if (KeyComponent.this.draggable) {
					try {
						Transferable transferable = new KeyTransferable(
								KeyComponent.this.getKey());
						e.startDrag(DragSource.DefaultLinkNoDrop, transferable,
								dsListener);
					} catch (final Throwable t) {
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								StackTrace.showStackTrace(t);
							}
						});
					}
				}
			}
		};
		final DropTargetListener dtListener = new DropTargetListener() {
			public void dragEnter(DropTargetDragEvent e) {
				if (isDragOk(e) == false) {
					e.rejectDrag();
					return;
				}
				e.acceptDrag(DnDConstants.ACTION_COPY);
			}

			public void dragOver(DropTargetDragEvent e) {
				if (isDragOk(e) == false) {
					e.rejectDrag();
					return;
				}
				e.acceptDrag(DnDConstants.ACTION_COPY);
			}

			public void dropActionChanged(DropTargetDragEvent e) {
				if (isDragOk(e) == false) {
					e.rejectDrag();
					return;
				}
				e.acceptDrag(DnDConstants.ACTION_COPY);
			}

			public void dragExit(DropTargetEvent e) {
			}

			private boolean isDragOk(DropTargetDragEvent e) {
				DataFlavor[] flavors = KeyTransferable.flavors;
				DataFlavor chosen = null;
				for (int i = 0; i < flavors.length; i++) {
					if (e.isDataFlavorSupported(flavors[i])) {
						chosen = flavors[i];
						break;
					}
				}
				if (chosen == null) {
					return false;
				}
				int sa = e.getSourceActions();
				if ((sa & DnDConstants.ACTION_COPY) == 0)
					return false;
				return true;
			}

			public void drop(DropTargetDropEvent e) {
				DataFlavor[] flavors = KeyTransferable.flavors;
				DataFlavor chosen = null;
				for (int i = 0; i < flavors.length; i++) {
					if (e.isDataFlavorSupported(flavors[i])) {
						chosen = flavors[i];
						break;
					}
				}
				if (chosen == null) {
					e.rejectDrop();
					return;
				}
				int sa = e.getSourceActions();
				if ((sa & DnDConstants.ACTION_COPY) == 0) {
					e.rejectDrop();
					return;
				}
				Object data = null;
				try {
					e.acceptDrop(DnDConstants.ACTION_COPY);
					data = e.getTransferable().getTransferData(chosen);
					if (data == null)
						throw new NullPointerException();
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							StackTrace.showStackTrace(t);
						}
					});
					e.dropComplete(false);
					return;
				}
				if (data instanceof Key) {
					final Key sourceKey = (Key) data;
					final Key targetKey = KeyComponent.this.getKey();
					if (!sourceKey.equals(targetKey)) {
						KeyComponent.this.getDiagram().getMartTab()
								.getSchemaTabSet().requestCreateRelation(
										sourceKey, targetKey);
						e.dropComplete(true);
						return;
					}
				}
				e.dropComplete(false);
			}
		};
		final DropTarget dropTarget = new DropTarget(this,
				DnDConstants.ACTION_COPY, dtListener, true);
		dragSource.createDefaultDragGestureRecognizer(this,
				DnDConstants.ACTION_COPY, dgListener);
	}

	private Key getKey() {
		return (Key) this.getObject();
	}

	/**
	 * For drag-and-drop, this returns the object that will be dropped onto the
	 * target when drag-and-drop starts from this key.
	 * 
	 * @return the key the user 'picked up' with the mouse.
	 */
	public Key getDraggedKey() {
		return this.getKey();
	}

	public void recalculateDiagramComponent() {
		// Removes all columns.
		this.removeAll();

		// Create the background colour.
		if (this.getKey() instanceof PrimaryKey)
			this.setBackground(KeyComponent.PK_BACKGROUND_COLOUR);
		else
			this.setBackground(KeyComponent.FK_BACKGROUND_COLOUR);

		// Add the labels for each column.
		final StringBuffer sb = new StringBuffer();
		for (final Iterator i = this.getKey().getColumns().iterator(); i
				.hasNext();) {
			final Column column = (Column) i.next();
			sb
					.append(column instanceof DataSetColumn ? ((DataSetColumn) column)
							.getModifiedName()
							: column.getName());
			if (i.hasNext())
				sb.append(", ");
		}
		final JLabel label = new JLabel(sb.toString());
		label.setFont(KeyComponent.PLAIN_FONT);
		this.layout.setConstraints(label, this.constraints);
		this.add(label);
	}

	public void setDraggable(boolean draggable) {
		this.draggable = draggable;
	}

	public static class KeyTransferable implements Transferable {
		public static final DataFlavor keyFlavor = new DataFlavor(Key.class,
				"MartBuilder Schema Key") {
		};

		public static final DataFlavor[] flavors = { KeyTransferable.keyFlavor };

		private static final List flavorList = Arrays.asList(flavors);

		public synchronized DataFlavor[] getTransferDataFlavors() {
			return flavors;
		}

		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return (flavorList.contains(flavor));
		}

		private Key key;

		public KeyTransferable(Key key) {
			this.key = key;
		}

		public Object getTransferData(DataFlavor flavor)
				throws UnsupportedFlavorException, IOException {
			if (KeyTransferable.keyFlavor.equals(flavor)) {
				return this.key;
			} else {
				throw new UnsupportedFlavorException(flavor);
			}
		}
	}
}
