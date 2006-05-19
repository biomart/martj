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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.biomart.builder.exceptions.AlreadyExistsException;
import org.biomart.builder.exceptions.AssociationException;
import org.biomart.builder.exceptions.BuilderException;
import org.biomart.builder.exceptions.MartBuilderInternalError;
import org.biomart.builder.resources.BuilderBundle;

/**
 * <p>
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
 * @version 0.1.8, 19th May 2006
 * @since 0.1
 */
public interface SchemaGroup extends Schema {
	/**
	 * Returns the set of schema members of this schema group. It will never
	 * return null but may return an empty set.
	 * 
	 * @return the set of schemas in this schema group.
	 */
	public Collection getSchemas();

	/**
	 * Adds a schema to this partition. No check is made to see if the new
	 * schema is actually identical to the base one in terms of structure. An
	 * exception will be thrown if you try to nest schema groups inside other
	 * ones.
	 * 
	 * @param schema
	 *            the {@link Schema to add as a new partition.
	 * @throws AlreadyExistsException
	 *             if the schema has already been added here.
	 * @throws AssociationException
	 *             if the schema to be added is a schema group.
	 */
	public void addSchema(Schema schema) throws AlreadyExistsException,
			AssociationException;

	/**
	 * Removes the schema from this group.
	 * 
	 * @param schema
	 *            the schema to remove.
	 */
	public void removeSchema(Schema schema);

	/**
	 * The generic implementation uses a simple collection of schemas to do the
	 * work.
	 */
	public class GenericSchemaGroup extends GenericSchema implements
			SchemaGroup {
		private final List schemas = new ArrayList();

		/**
		 * The constructor creates a schema group with the given name.
		 * 
		 * @param name
		 *            the name for this new schema group.
		 */
		public GenericSchemaGroup(String name) {
			super(name);
		}

		public Schema replicate(String newName) {
			SchemaGroup newGroup = new GenericSchemaGroup(newName);
			try {
				for (Iterator i = this.schemas.iterator(); i.hasNext();)
					newGroup.addSchema((Schema) i.next());
			} catch (Throwable t) {
				throw new MartBuilderInternalError(t);
			}
			return newGroup;
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * The schema group delegates this call to each of its members in turn.
		 * Then replication is used to copy all the tables etc. from the first
		 * schema in the list of members and set them up as the group's own.
		 */
		public void synchronise() throws SQLException, BuilderException {
			// Synchronise our members.
			for (Iterator i = this.schemas.iterator(); i.hasNext();) {
				((Schema) i.next()).synchronise();
			}
			// Update our own list by using replication.
			if (!this.schemas.isEmpty())
				((Schema) this.schemas.get(0)).replicateContents(this);
		}

		public Collection getSchemas() {
			return this.schemas;
		}

		public void addSchema(Schema schema) throws AlreadyExistsException,
				AssociationException {
			// Check the schema doesn't already exist, and isn't a group itself.
			if (this.schemas.contains(schema))
				throw new AlreadyExistsException(BuilderBundle
						.getString("schemaExists"), schema.getName());
			if (schema instanceof SchemaGroup)
				throw new AssociationException(BuilderBundle
						.getString("nestedSchema"));

			// Add it.
			this.schemas.add(schema);
		}

		public void removeSchema(Schema schema) {
			this.schemas.remove(schema);
		}
	}
}
