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

package org.biomart.builder.view.gui.diagrams;

import java.awt.AWTEvent;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JViewport;

import org.biomart.builder.view.gui.MartTabSet.MartTab;
import org.biomart.builder.view.gui.diagrams.components.DiagramComponent;
import org.biomart.builder.view.gui.diagrams.contexts.DiagramContext;
import org.biomart.common.model.Table;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.common.view.gui.ComponentImageSaver;
import org.biomart.common.view.gui.ComponentPrinter;

/**
 * A diagram represents a collection of database components. It usually contains
 * components that are tables, which themselves contain other objects which are
 * keys and columns. The diagram also contains components which are relations,
 * which link key objects. The diagram remembers all this, and provides a
 * context-menu handling for the components it displays. {@link DiagramContext}
 * listeners can be attached to the diagram to customise context menu rendering,
 * and also to customise rendering of the individual components, for instance in
 * order to apply alternative colour schemes.
 * <p>
 * Specific extensions of this basic diagram class handle the decisions as to
 * what to add and what to remove from the diagram. This base class simply deals
 * with the context menus and display of components in the diagram.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.1
 */
public abstract class Diagram extends JPanel {
	// OK to use maps as it gets cleared out each time, the keys never change.
	private final Map componentMap = new HashMap();

	private boolean contextChanged = false;

	private DiagramContext diagramContext;

	private MartTab martTab;

	/**
	 * Creates a new diagram which belongs inside the given mart tab and uses
	 * the given layout manager. The {@link MartTab#getMart()} method will be
	 * used to work out which mart is being interacted with when the user
	 * selects items in the context menus attached to components in this
	 * diagram.
	 * 
	 * @param layout
	 *            the layout manager to use to layout the diagram.
	 * @param martTab
	 *            the mart tab this diagram will use to discover which mart is
	 *            currently visible when working out where to send events.
	 */
	public Diagram(final LayoutManager layout, final MartTab martTab) {
		// Set us up with the layout.
		super(layout);

		Log.debug("Creating new diagram of type " + this.getClass().getName());

		// Enable mouse events to be picked up all over the diagram.
		this.enableEvents(AWTEvent.MOUSE_EVENT_MASK);

		// Remember our settings.
		this.martTab = martTab;
	}

	/**
	 * Creates a new diagram which belongs inside the given mart tab and uses
	 * the given layout manager. The {@link MartTab#getMart()} method will be
	 * used to work out which mart is being interacted with when the user
	 * selects items in the context menus attached to components in this
	 * diagram.
	 * <p>
	 * This constructor is the same as the other one, but defaults to an
	 * instance of {@link LinearLayout} to handle layout of the diagram.
	 * 
	 * @param martTab
	 *            the mart tab this diagram will use to discover which mart is
	 *            currently visible when working out where to send events.
	 */
	public Diagram(final MartTab martTab) {
		this(new LinearLayout(), martTab);
	}

	private Table askUserForTable() {
		// Pop up a dialog box with a list of tables in it, and ask the
		// user to select one. Only tables which appear in this diagram will
		// be in the list.

		// First, work out what tables are in this diagram.
		final Set tables = new TreeSet();
		for (final Iterator i = this.componentMap.keySet().iterator(); i
				.hasNext();) {
			final Object o = i.next();
			if (o instanceof Table)
				tables.add(o);
		}

		// Now, create the choices box, display it, and return the one
		// that the user selected. If the user didn't select anything, or
		// cancelled the choice, this will return null.
		return (Table) JOptionPane.showInputDialog(null, Resources
				.get("findTableName"), Resources.get("questionTitle"),
				JOptionPane.QUESTION_MESSAGE, null, tables.toArray(), null);
	}

	private JPopupMenu populateContextMenu(JPopupMenu contextMenu) {
		// This is the basic context menu that appears no matter where the user
		// clicks.

		// If the menu is not empty, add a separator before we
		// add the context stuff.
		if (contextMenu.getComponentCount() > 0)
			contextMenu.addSeparator();

		// Add an item that allows the user to search for a particular
		// table in the diagram, and scroll to that table when selected.
		final JMenuItem find = new JMenuItem(Resources.get("findTableTitle"));
		find.setMnemonic(Resources.get("findTableMnemonic").charAt(0));
		find.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				final Table table = Diagram.this.askUserForTable();
				if (table != null)
					Diagram.this.findObject(table);
			}
		});
		contextMenu.add(find);

		contextMenu.addSeparator();

		// Add an item that allows the user to save this diagram as an image.
		final JMenuItem save = new JMenuItem(Resources.get("saveDiagramTitle"),
				new ImageIcon(Resources.getResourceAsURL("save.gif")));
		save.setMnemonic(Resources.get("saveDiagramMnemonic").charAt(0));
		save.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				Diagram.this.saveDiagram();
			}
		});
		contextMenu.add(save);

		// Add an item that allows the user to print this diagram.
		final JMenuItem print = new JMenuItem(Resources
				.get("printDiagramTitle"), new ImageIcon(Resources
				.getResourceAsURL("print.gif")));
		print.setMnemonic(Resources.get("printDiagramMnemonic").charAt(0));
		print.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				Diagram.this.printDiagram();
			}
		});
		contextMenu.add(print);

		// Return the completed context menu.
		return contextMenu;
	}

	private void printDiagram() {
		new ComponentPrinter(this).print();
	}

	private void saveDiagram() {
		new ComponentImageSaver(this).save();
	}

	/**
	 * Finds out what database object this diagram should treat as the
	 * background object for the entire diagram. In other words, if you want the
	 * background of the diagram to behave as if the user had clicked on a
	 * specific database object, this is the method you would override to
	 * provide that object. The default is to return null, ie. the diagram has
	 * no background object.
	 * 
	 * @return the background database object for this diagram.
	 */
	protected Object getContextMenuBaseObject() {
		return null;
	}

	protected void paintComponent(final Graphics g) {
		// If the context has changed since last time we
		// painted anything, update all our components'
		// appearance first.
		if (this.contextChanged) {
			this.repaintDiagram();
			this.contextChanged = false;
		}
		// Now, repaint as normal.
		super.paintComponent(g);
	}

	protected void processMouseEvent(final MouseEvent evt) {
		boolean eventProcessed = false;

		// Is it a right-click?
		if (evt.isPopupTrigger()) {

			// Obtain the basic context menu for this diagram.
			final JPopupMenu contextMenu = new JPopupMenu();

			// Extend the basic menu by delegating to the context, using the
			// background database object of this diagram to provide the
			// options.
			if (this.getDiagramContext() != null)
				this.getDiagramContext().populateContextMenu(contextMenu,
						this.getContextMenuBaseObject());

			// Add the common diagram stuff.
			this.populateContextMenu(contextMenu);

			// If our context menu actually has anything in it now, display it.
			if (contextMenu.getComponentCount() > 0)
				contextMenu.show(this, evt.getX(), evt.getY());

			// Mark the event as processed.
			eventProcessed = true;
		}

		// Pass the event on up if we're not interested.
		if (!eventProcessed)
			super.processMouseEvent(evt);
	}

	/**
	 * This method is called to make sure the appearance of the diagram is
	 * up-to-date prior to a repaint. This is called by
	 * {@link #repaintDiagram()} before the actual repaint is done.
	 * <p>
	 * Usually, the only thing this call would do is to set the background
	 * colour of the diagram.
	 */
	protected abstract void updateAppearance();

	/**
	 * Adds a component to this diagram. First it adds the component to the
	 * internal map, allowing users to later query which component is related to
	 * which database object. Second, it adds the component to the layout for
	 * this diagram.
	 * 
	 * @param comp
	 *            the component to add.
	 */
	public void addDiagramComponent(final DiagramComponent comp) {
		this.componentMap.put(comp.getObject(), comp);
		this.componentMap.putAll(comp.getSubComponents());
		super.add((JComponent) comp);
	}

	/**
	 * Override this method to actually do the work of recalculating which
	 * components should appear in the diagram. The method should first clear
	 * out all the old components from the diagram, as this will not have been
	 * done already. On return, the diagram should contain a new set of
	 * components, or an updated set of components that correctly reflects its
	 * current state.
	 */
	public abstract void doRecalculateDiagram();

	/**
	 * Given a particular model object, lookup the diagram component that it
	 * represents, then scroll the diagram so that it is centred on that diagram
	 * component. This depends on the diagram being held within a
	 * {@link JScrollPane} - if it isn't, this method will do nothing.
	 * 
	 * @param object
	 *            the database object to locate and scroll to.
	 */
	public void findObject(final Object object) {
		// Don't do it if the object is null or if we are not in a viewport.
		if (object == null || !(this.getParent() instanceof JViewport))
			return;

		// Ensure the diagram is valid and the correct size.
		this.resizeDiagram();

		// Obtain the scrollpane view of this diagram.
		final JViewport viewport = (JViewport) this.getParent();

		// Look up the diagram component for the model object.
		final JComponent comp = (JComponent) this.getDiagramComponent(object);

		// If the model object is not in this diagram, don't scroll to it!
		if (comp == null)
			return;

		// Work out the location of the diagram component.
		final Point compLocation = comp.getLocation();
		final Dimension compSize = comp.getPreferredSize();

		// Recursively add on the parent components to the location, until
		// the location coordinates become relevant to the diagram itself.
		Container parent = comp.getParent();
		while (parent != this) {
			compLocation.setLocation(compLocation.x + parent.getX(),
					compLocation.y + parent.getY());
			parent = parent.getParent();
		}

		// Work out the centre point of the diagram component, based on its
		// location.
		final Point compCentre = new Point(compLocation.x + compSize.width / 2,
				compLocation.y + compSize.height / 2);

		// How big is the scrollpane view we are being seen through?
		final Dimension viewSize = viewport.getExtentSize();

		// Work out the top-left coordinate of the area of diagram that should
		// appear in the scrollpane if this diagram component is to appear in
		// the absolute centre.
		int newViewPointX = compCentre.x - viewSize.width / 2;
		int newViewPointY = compCentre.y - viewSize.height / 2;

		// Move the scrollpoint if it goes off the top-left of the diagram.
		if (newViewPointX - viewSize.width / 2 < 0)
			newViewPointX = 0;
		if (newViewPointY - viewSize.height / 2 < 0)
			newViewPointY = 0;

		// Move the scrollpoint if it goes off the bottom-right.
		if (newViewPointX + viewSize.width / 2 > parent.getWidth())
			newViewPointX = parent.getWidth() - viewSize.width;
		if (newViewPointY + viewSize.height / 2 > parent.getHeight())
			newViewPointY = parent.getHeight() - viewSize.height;

		// Scroll to that position.
		viewport.setViewPosition(new Point(newViewPointX, newViewPointY));
	}

	/**
	 * Looks up the diagram component in this diagram that is related to the
	 * specified database object. If there is no component related to that
	 * object, then null is returned, otherwise the component is returned.
	 * 
	 * @param object
	 *            the database object to look up the component for.
	 * @return the diagram component that represents that database object in
	 *         this diagram, or null if that model object is not in this diagram
	 *         at all.
	 */
	public DiagramComponent getDiagramComponent(final Object object) {
		return (DiagramComponent) this.componentMap.get(object);
	}

	/**
	 * Returns the diagram context that is being used to customise colours and
	 * context menus for this diagram.
	 * 
	 * @return the diagram context that is being used.
	 */
	public DiagramContext getDiagramContext() {
		return this.diagramContext;
	}

	/**
	 * Obtain a reference to the mart tab this diagram was registered with.
	 * 
	 * @return the mart tab provided at construction time for this diagram.
	 */
	public MartTab getMartTab() {
		return this.martTab;
	}

	/**
	 * This method is called when the diagram needs to be cleared and
	 * repopulated. It remembers the states of all the components in the
	 * diagram, then delegates to {@link #doRecalculateDiagram()} to do the
	 * actual work of clearing out and repopulating the diagram. Finally, it
	 * reapplies the states remembered to any components in the new diagram that
	 * match the components in the old diagram using the
	 * {@link Object#equals(Object)} method.
	 */
	public void recalculateDiagram() {
		Log.debug("Recalculating diagram");
		// Remember all the existing diagram component states.
		final Map states = new HashMap();
		for (final Iterator i = this.componentMap.entrySet().iterator(); i
				.hasNext();) {
			final Map.Entry entry = (Map.Entry) i.next();
			final Object object = entry.getKey();
			final DiagramComponent comp = (DiagramComponent) entry.getValue();

			// If the component actually exists, which it may not if the
			// diagram has been dynamically updated elsewhere, remember the
			// state, else remove the current component because it is not
			// relevant.
			if (comp != null)
				states.put(object, comp.getState());
			else
				i.remove();
		}

		// Delegate to do the actual diagram clear-and-repopulate.
		this.doRecalculateDiagram();

		// Reapply all the states. The methods of the Map interface use equals()
		// to compare objects, so any objects in the new diagram which match
		// the old objects in the old diagram will inherit the state from the
		// old objects.
		for (final Iterator i = states.entrySet().iterator(); i.hasNext();) {
			final Map.Entry entry = (Map.Entry) i.next();
			final Object object = entry.getKey();
			final DiagramComponent comp = (DiagramComponent) this.componentMap
					.get(object);
			if (comp != null)
				comp.setState(entry.getValue());
		}

		// Update appearances of components.
		this.repaintDiagram();
	}

	public void removeAll() {
		// Do what the parent JComponent would do.
		super.removeAll();

		// Clear our internal lookup map.
		this.componentMap.clear();
	}

	/**
	 * This method first calls {@link #updateAppearance()} on this diagram, then
	 * walks through the components in the diagram, and calls
	 * {@link DiagramComponent#updateAppearance()} on each in turn. This has the
	 * effect of updating the appearance of every component and causing it to
	 * redraw. It does not recalculate the location of any of these components,
	 * neither does it recalculate which components are displayed in the diagram
	 * at present.
	 * <p>
	 * This method does not resize the diagram to fit components, so do not use
	 * it if the component size is likely to have changed (eg. show/hide columns
	 * on a table). Use {@link #recalculateDiagram()} instead.
	 */
	public void repaintDiagram() {
		this.updateAppearance();
		for (final Iterator i = this.componentMap.values().iterator(); i
				.hasNext();)
			((DiagramComponent) i.next()).updateAppearance();
		this.repaint(this.getVisibleRect());
	}

	/**
	 * Work out the minimum size for this diagram, resize ourselves to that
	 * size, then validate ourselves so our contents get laid out correctly.
	 */
	public void resizeDiagram() {
		// Reset our size to the minimum.
		this.setSize(this.getMinimumSize());
		// Update ourselves.
		this.validate();
	}

	/**
	 * Sets the diagram context that will be used to customise colours and
	 * context menus for this diagram.
	 * 
	 * @param diagramContext
	 *            the diagram context to use.
	 */
	public void setDiagramContext(final DiagramContext diagramContext) {
		Log.debug("Switching diagram context");
		// Apply it to ourselves.
		this.diagramContext = diagramContext;
		this.contextChanged = true;
	}
}
