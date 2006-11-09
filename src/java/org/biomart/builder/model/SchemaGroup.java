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

package org.biomart.builder.model;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.biomart.common.exceptions.AssociationException;
import org.biomart.common.exceptions.BioMartError;
import org.biomart.common.exceptions.DataModelException;
import org.biomart.common.model.Schema;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;

/**
 * A schema group represents a collection of schema objects which all have
 * exactly the same table names and column names.
 * <p>
 * When generating a mart from this later, the mart will have an extra column on
 * every table indicating which individual schema within the group the row came
 * from.
 * <p>
 * As the schema group is a schema itself, all operations on it which modify the
 * structure of the tables are passed on to each of its member schema objects in
 * turn.
 * <p>
 * The structure of the tables etc. in the schema group is a copy of the
 * structure in the first member schema.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.1
 */
public interface SchemaGroup extends Schema {
	/**
	 * Adds a schema to this group. No check is made to see if the new schema is
	 * actually identical to the base one in terms of structure. An exception
	 * will be thrown if you try to nest schema groups inside other ones.
	 * 
	 * @param schema
	 *            the schema to add to the group.
	 * @throws AssociationException
	 *             if the schema to be added is a schema group.
	 */
	public void addSchema(Schema schema) throws AssociationException;

	/**
	 * Returns the set of schema members of this schema group. It will never
	 * return <tt>null</tt> but may return an empty set.
	 * 
	 * @return the set of schemas in this schema group.
	 */
	public Collection getSchemas();

	/**
	 * Removes the schema from this group.
	 * 
	 * @param schema
	 *            the schema to remove.
	 */
	public void removeSchema(Schema schema);

	/**
	 * The generic implementation uses a simple collection to store the schemas.
	 */
	public class GenericSchemaGroup extends GenericSchema implements
			SchemaGroup {
		private final Set schemas = new HashSet();

		/**
		 * The constructor creates a schema group with the given name.
		 * 
		 * @param name
		 *            the name for this new schema group.
		 */
		public GenericSchemaGroup(final String name) {
			super(name);
		}

		public void addSchema(final Schema schema) throws AssociationException {
			Log.debug("Adding " + schema + " to " + this.getName());
			// Check the schema isn't a group itself.
			if (schema instanceof SchemaGroup)
				throw new AssociationException(Resources.get("nestedSchema"));

			// Add it.
			this.schemas.add(schema);
		}

		public Collection getSchemas() {
			return this.schemas;
		}

		public void removeSchema(final Schema schema) {
			Log.debug("Removing " + schema + " from "
					+ this.getName());
			this.schemas.remove(schema);
		}

		public Schema replicate(final String newName) {
			Log.debug("Replicating " + this + " as " + newName);
			throw new BioMartError(Resources.get("noSchemaGroupReplication"));
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * The schema group delegates this call to each of its members in turn.
		 * Then replication is used to copy all the tables etc. from the first
		 * schema in the list of members and set them up as the group's own.
		 */
		public void synchronise() throws SQLException, DataModelException {
			// Synchronise our members.
			Log.info(Resources.get("logSchGroupSyncing", ""
					+ this.getName()));
			for (final Iterator i = this.schemas.iterator(); i.hasNext();)
				((Schema) i.next()).synchronise();
			// Update our own list by using replication.
			Log.info(Resources.get("logSchGroupCopying", ""
					+ this.getName()));
			if (!this.schemas.isEmpty())
				((Schema) this.schemas.iterator().next())
						.replicateContents(this);
		}
	}
}
