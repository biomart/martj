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

import java.awt.AWTEvent;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JViewport;

import org.biomart.builder.model.Table;
import org.biomart.builder.resources.BuilderBundle;

/**
 * <p>
 * A diagram represents a database schema. It contains objects that are tables,
 * which themselves contain other objects which are keys and columns. The
 * diagram also contains objects which are relations, which link key objects.
 * The diagram remembers all this, and provides context-menu handling for the
 * menu objects. {@link DiagramContext} objects can be attached to the diagram
 * to intercept context menu rendering, and also to intercept rendering of the
 * individual components, to allow customised colour schemes to be applied.
 * <p>
 * Specific extensions of this basic diagram class handle the actual adding and
 * removing of tables and relations from the diagram. This basic diagram class
 * handles only the display of items that are added to it - it doesn't work out
 * what those items should be.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.18, 2nd June 2006
 * @since 0.1
 */
public abstract class Diagram extends JPanel {
	private DiagramContext diagramContext;

	private DataSetTabSet datasetTabSet;

	// Use double-list to prevent problems with changing hashcodes.
	private List[] componentMap = new List[]{new ArrayList(), new ArrayList()};

	/**
	 * Creates a new diagram which belongs inside the given dataset tabset. The
	 * tabset will be the one used to handle events generated by the user
	 * selecting items in the context menus attached to objects in this diagram.
	 * 
	 * @param datasetTabSet
	 *            the tabset this diagram will delegate user events to.
	 */
	public Diagram(DataSetTabSet datasetTabSet) {
		// Set us up with a nice circular layout.
		super(new RadialLayout());

		// Enable mouse events to be picked up all over the diagram.
		this.enableEvents(AWTEvent.MOUSE_EVENT_MASK);

		// Remember our settings.
		this.datasetTabSet = datasetTabSet;
	}

	/**
	 * Obtain a reference to the dataset tabset this diagram was registered
	 * with.
	 * 
	 * @return the dataset tabset provided at construction time for this
	 *         diagram.
	 */
	protected DataSetTabSet getDataSetTabSet() {
		return this.datasetTabSet;
	}

	public void removeAll() {
		// Do what the parent JComponent would do.
		super.removeAll();

		// Clear our internal lookup map.
		this.componentMap[0].clear();
		this.componentMap[1].clear();
	}

	/**
	 * Adds a component to this diagram. First it adds the component to the
	 * internal map, allowing users to later query which component is related to
	 * which model object. Second, it adds the component to the layout for this
	 * diagram.
	 * 
	 * @param comp
	 *            the component to add.
	 */
	public void addDiagramComponent(DiagramComponent comp) {
		this.componentMap[0].add(comp.getObject());
		this.componentMap[1].add(comp);
		this.componentMap[0].addAll(comp.getSubComponents()[0]);
		this.componentMap[1].addAll(comp.getSubComponents()[1]);
		super.add((JComponent) comp);
	}

	/**
	 * Looks up the diagram component in this diagram that is related to the
	 * specified model object. If there is no component related to that object,
	 * then null is returned, otherwise the component is returned.
	 * 
	 * @param object
	 *            the model object to look up the component for.
	 * @return the diagram component that represents that model object in this
	 *         diagram, or null if that model object is not in this diagram at
	 *         all.
	 */
	public DiagramComponent getDiagramComponent(Object object) {
		int index = this.componentMap[0].indexOf(object);
		return (DiagramComponent) this.componentMap[1].get(index);
	}

	/**
	 * Finds out what model object this diagram should treat as the background
	 * object for the entire diagram. In other words, if you want the background
	 * of the diagram to behave as if the user had clicked on a specific model
	 * object, this is the method you would override to provide that object. The
	 * default is to return null, ie. the diagram has no background object.
	 * 
	 * @return the background model object for this diagram.
	 */
	protected Object getContextMenuBaseObject() {
		return null;
	}

	/**
	 * Given a particular model object, lookup the diagram component that it
	 * represents, then scroll the diagram so that it is centred on that diagram
	 * component. This depends on the diagram being held within a
	 * {@link JScrollPane} - if it isn't, you'll get exceptions when using this
	 * method.
	 * 
	 * @param object
	 *            the model object to locate and scroll to.
	 */
	public void findObject(Object object) {
		// Don't do it if the object is null.
		if (object == null)
			return;
		
		// Ensure the diagram is valid and the correct size.
		this.resizeDiagram();

		// Obtain the scrollpane view of this diagram.
		JViewport viewport = (JViewport) this.getParent();

		// Look up the diagram component for the model object.
		JComponent comp = (JComponent) this.getDiagramComponent(object);

		// If the model object is not in this diagram, don't scroll to it!
		if (comp == null)
			return;

		// Work out the location of the diagram component.
		Point compLocation = comp.getLocation();
		Dimension compSize = comp.getPreferredSize();

		// Recursively add on the parent components to the location, until
		// the location coordinates become relevant to the diagram itself.
		Container parent = comp.getParent();
		while (parent != this) {
			compLocation.setLocation(compLocation.x + (int) parent.getX(),
					compLocation.y + (int) parent.getY());
			parent = parent.getParent();
		}

		// Work out the centre point of the diagram component, based on its
		// location.
		Point compCentre = new Point(compLocation.x + (compSize.width / 2),
				compLocation.y + (compSize.height / 2));

		// How big is the scrollpane view we are being seen through?
		Dimension viewSize = viewport.getExtentSize();

		// Work out the top-left coordinate of the area of diagram that should
		// appear in the scrollpane if this diagram component is to appear in
		// the absolute centre.
		int newViewPointX = compCentre.x - (viewSize.width / 2);
		int newViewPointY = compCentre.y - (viewSize.height / 2);

		// Scroll to that position.
		viewport.setViewPosition(new Point(newViewPointX, newViewPointY));
	}

	private Table askUserForTable() {
		// Pop up a dialog box with a list of tables in it, and ask the
		// user to select one. Only tables which appear in this diagram will
		// appear.

		// First, work out what tables are in this diagram.
		Set tables = new TreeSet();
		for (Iterator i = this.componentMap[0].iterator(); i.hasNext();) {
			Object o = i.next();
			if (o instanceof Table)
				tables.add(o);
		}

		// Now, create the choices box, display it, and return the one
		// that the user selected. If the user didn't select anything, or
		// cancelled the choice, this will return null.
		return (Table) JOptionPane.showInputDialog(this.datasetTabSet,
				BuilderBundle.getString("findTableName"), BuilderBundle
						.getString("questionTitle"),
				JOptionPane.QUESTION_MESSAGE, null, tables.toArray(), null);
	}

	private JPopupMenu getContextMenu() {
		// This is the basic context menu that appears no matter where the user
		// clicks.

		// Create an empty context menu to start with.
		JPopupMenu contextMenu = new JPopupMenu();

		// Add an item that allows the user to search for a particular
		// table in the diagram, and scroll to that table when selected.
		JMenuItem find = new JMenuItem(BuilderBundle
				.getString("findTableTitle"));
		find
				.setMnemonic(BuilderBundle.getString("findTableMnemonic")
						.charAt(0));
		find.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Table table = askUserForTable();
				if (table != null)
					findObject(table);
			}
		});
		contextMenu.add(find);

		// Return the completed context menu.
		return contextMenu;
	}

	protected void processMouseEvent(MouseEvent evt) {
		boolean eventProcessed = false;

		// Is it a right-click?
		if (evt.isPopupTrigger()) {

			// Obtain the basic context menu for this diagram, that appears no
			// matter
			// where the user clicked.
			JPopupMenu contextMenu = this.getContextMenu();

			// Extend the basic diagram by delegating to the context, using the
			// basic
			// background object of this diagram to provide the options.
			this.getDiagramContext().populateContextMenu(contextMenu,
					this.getContextMenuBaseObject());

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
	 * Sets the diagram context that will be used to customise colours and
	 * context menus for this diagram.
	 * 
	 * @param diagramContext
	 *            the diagram context to use.
	 */
	public void setDiagramContext(DiagramContext diagramContext) {
		// Apply it to ourselves.
		this.diagramContext = diagramContext;

		// Use it straight away to update the appearance of all our
		// components.
		for (Iterator i = this.componentMap[1].iterator(); i.hasNext();)
			((DiagramComponent) i.next()).updateAppearance();
	}

	/**
	 * Returns the diagram context that is being used to customise colours and
	 * context menus for this diagram.
	 * 
	 * @param diagramContext
	 *            the diagram context that is being used.
	 */
	public DiagramContext getDiagramContext() {
		return this.diagramContext;
	}

	/**
	 * <p>
	 * This method is called when the diagram needs to be cleared and
	 * repopulated. It remembers the states of all the components in the
	 * diagram, then delegates to {@link #doRecalculateDiagram()} to do the
	 * actual work of clearing out and repopulating the diagram. Finally, it
	 * reapplies the states remembered to any objects in the new diagram that
	 * match the objects in the old diagram using the <tt>equals()</tt>
	 * method.
	 */
	public void recalculateDiagram() {
		// Remember all the existing diagram component states.
		Map states = new HashMap();
		List removed = new ArrayList();
		for (int i = 0; i < this.componentMap[0].size(); i++) {
			Object object = this.componentMap[0].get(i);
			DiagramComponent comp = (DiagramComponent) this.componentMap[1]
					.get(i);

			// If the component actually exists, which it may not if the
			// diagram has been dynamically updated elsewhere, remember the
			// state, else remove the current component because it is not
			// relevant.
			if (comp != null)
				states.put(object, comp.getState());
			else
				removed.add(object);
		}

		// Remove the removed ones.
		for (Iterator i = removed.iterator(); i.hasNext(); ) {
			int index = this.componentMap[0].indexOf(i.next());
			this.componentMap[0].remove(index);
			this.componentMap[1].remove(index);
		}
		
		// Delegate to do the actual diagram clear-and-repopulate.
		this.doRecalculateDiagram();

		// Reapply all the states. The methods of the Map interface use equals()
		// to compare objects, so any objects in the new diagram which match
		// the old objects in the old diagram will inherit the state from the
		// old objects.
		for (Iterator i = states.keySet().iterator(); i.hasNext();) {
			Object object = i.next();
			int index = this.componentMap[0].indexOf(object);
			if (index>=0) {
			DiagramComponent comp = (DiagramComponent) this.componentMap[1]
					.get(index);
			if (comp != null)
				comp.setState(states.get(object));
			}
		}
		
		// Finally, repaint it as by default a component only repaints
		// the area uncovered by the last action, but we have changed
		// the entire visible area, so need the entire visible area to 
		// be repainted.
		this.repaint(this.getVisibleRect());
	}

	/**
	 * Override this method to actually do the work of recalculating which
	 * objects should appear in the diagram. The method should first clear out
	 * all the old objects from the diagram, as this will not have been done
	 * already. On return, the diagram should contain a new set of objects, or
	 * an updated set of objects that correctly reflects its current state.
	 */
	public abstract void doRecalculateDiagram();

	/**
	 * <p>
	 * This method walks through each component in the diagram, and calls
	 * {@link DiagramComponent#updateAppearance()} on it. This has the effect of
	 * updating the appearance of every component and causing it to redraw. It
	 * does not recalculate the location of any of these components, neither
	 * does it recalculate which components are displayed in the diagram at
	 * present.
	 * <p>
	 * This method does not resize the diagram to fit components, so do not use
	 * it if the component size is likely to have changed (eg. show/hide columns
	 * on a table).
	 */
	public void repaintDiagram() {
		for (Iterator i = this.componentMap[1].iterator(); i.hasNext();)
			((DiagramComponent) i.next()).updateAppearance();
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
}
