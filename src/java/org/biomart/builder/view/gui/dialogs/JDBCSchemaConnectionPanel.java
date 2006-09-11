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

package org.biomart.builder.view.gui.dialogs;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;

import org.biomart.builder.controller.JDBCSchema;
import org.biomart.builder.controller.MartBuilderUtils;
import org.biomart.builder.model.Schema;
import org.biomart.builder.resources.Resources;
import org.biomart.builder.resources.SettingsCache;
import org.biomart.builder.view.gui.MartTabSet.MartTab;

/**
 * This connection panel implementation allows a user to define some JDBC
 * connection parameters, such as hostname, username, driver class and if
 * necessary the location where the driver can be found. It uses this to
 * construct a JDBC URL, dynamically, and ultimately creates a
 * {@link JDBCSchema} implementation which represents the connection.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.11, 11th September 2006
 * @since 0.1
 */
public class JDBCSchemaConnectionPanel extends SchemaConnectionPanel implements
		DocumentListener {
	// Please add any more default drivers that we support to this list. The
	// keys are the driver classnames, and the values are arrays of strings.
	// The first entry in the array should be the default port number for this
	// JDBC driver type, and the second entry should be an example JDBC URL.
	// Within the URL, the keywords <HOSTNAME>, <PORT> and <DATABASE> must all
	// appear in the order mentioned. Any other order will break the regex
	// replacement function elsewhere in this class.
	private static final Map DRIVER_MAP = new HashMap();

	private static final Map DRIVER_NAME_MAP = new HashMap();

	private static final long serialVersionUID = 1;

	static {
		// JDBC URL formats.
		JDBCSchemaConnectionPanel.DRIVER_MAP
				.put("com.mysql.jdbc.Driver", new String[] { "3306",
						"jdbc:mysql://<HOST>:<PORT>/<DATABASE>" });
		JDBCSchemaConnectionPanel.DRIVER_MAP.put(
				"oracle.jdbc.driver.OracleDriver", new String[] { "1531",
						"jdbc:oracle:thin:@<HOST>:<PORT>:<DATABASE>" });
		JDBCSchemaConnectionPanel.DRIVER_MAP.put("org.postgresql.Driver",
				new String[] { "5432",
						"jdbc:postgresql://<HOST>:<PORT>/<DATABASE>" });
		// Names.
		JDBCSchemaConnectionPanel.DRIVER_NAME_MAP.put(Resources
				.get("driverClassMySQL"), "com.mysql.jdbc.Driver");
		JDBCSchemaConnectionPanel.DRIVER_NAME_MAP.put(Resources
				.get("driverClassOracle"), "oracle.jdbc.driver.OracleDriver");
		JDBCSchemaConnectionPanel.DRIVER_NAME_MAP.put(Resources
				.get("driverClassPostgreSQL"), "org.postgresql.Driver");
	}

	private String currentJDBCURLTemplate;

	private JTextField database;

	private JTextField driverClass;

	private JTextField driverClassLocation;

	private JButton driverClassLocationButton;

	private JTextField host;

	private JFileChooser jarFileChooser;

	private JTextField jdbcURL;

	private MartTab martTab;

	private JPasswordField password;

	private JFormattedTextField port;

	private JComboBox predefinedDriverClass;

	private JTextField schemaName;

	private JTextField username;

	/**
	 * This constructor creates a panel with all the fields necessary to
	 * construct a {@link JDBCSchema} instance, save the name which will be
	 * passed in elsewhere. It optionally takes a template which is used to
	 * populate the fields of the panel. If this template is null, then defaults
	 * are used instead.
	 * <p>
	 * You must call {@link #resetFields(Schema)} before displaying this panel,
	 * otherwise the values displayed are not defined and may result in
	 * unpredictable behaviour.
	 * 
	 * @param martTab
	 *            the mart tab to which the schema to be created or modified
	 *            belongs.
	 */
	public JDBCSchemaConnectionPanel(final MartTab martTab) {
		super();

		// Remember the schema tabset.
		this.martTab = martTab;

		// Create the layout manager for this panel.
		final GridBagLayout gridBag = new GridBagLayout();
		this.setLayout(gridBag);

		// Create constraints for labels that are not in the last row.
		final GridBagConstraints labelConstraints = new GridBagConstraints();
		labelConstraints.gridwidth = GridBagConstraints.RELATIVE;
		labelConstraints.fill = GridBagConstraints.HORIZONTAL;
		labelConstraints.anchor = GridBagConstraints.LINE_END;
		labelConstraints.insets = new Insets(0, 2, 0, 0);
		// Create constraints for fields that are not in the last row.
		final GridBagConstraints fieldConstraints = new GridBagConstraints();
		fieldConstraints.gridwidth = GridBagConstraints.REMAINDER;
		fieldConstraints.fill = GridBagConstraints.NONE;
		fieldConstraints.anchor = GridBagConstraints.LINE_START;
		fieldConstraints.insets = new Insets(0, 1, 0, 2);
		// Create constraints for labels that are in the last row.
		final GridBagConstraints labelLastRowConstraints = (GridBagConstraints) labelConstraints
				.clone();
		labelLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;
		// Create constraints for fields that are in the last row.
		final GridBagConstraints fieldLastRowConstraints = (GridBagConstraints) fieldConstraints
				.clone();
		fieldLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;

		// Create all the useful fields in the dialog box.
		this.driverClassLocation = new JTextField(30);
		this.driverClassLocationButton = new JButton(Resources
				.get("browseButton"));
		this.jdbcURL = new JTextField(40);
		this.host = new JTextField(20);
		this.port = new JFormattedTextField(new DecimalFormat("0"));
		this.port.setColumns(4);
		this.database = new JTextField(10);
		this.schemaName = new JTextField(10);
		this.username = new JTextField(10);
		this.password = new JPasswordField(10);

		// The driver class box displays the currently selected driver
		// class. As it changes, other fields become highlighted.
		this.driverClass = new JTextField(20);
		this.driverClass.getDocument().addDocumentListener(this);
		this.driverClass.setText(null); // Force into default state.

		// The predefined driver class box displays everything we know
		// about by default, as defined by the map at the start of this class.
		this.predefinedDriverClass = new JComboBox(
				JDBCSchemaConnectionPanel.DRIVER_NAME_MAP.keySet().toArray(
						new String[0]));
		this.predefinedDriverClass.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				// This method is called when the driver class field is
				// changed, either by the user typing in it, or using
				// the drop-down to select a predefine value.

					// Work out which database was selected.
					final String classType = (String) JDBCSchemaConnectionPanel.this.predefinedDriverClass
							.getSelectedItem();

					// Use it to look up the default class for that database,
					// then reset the drop-down to nothing-selected.
					if (!JDBCSchemaConnectionPanel.this.isEmpty(classType)) {
						final String driverClassName = (String) JDBCSchemaConnectionPanel.DRIVER_NAME_MAP
								.get(classType);
						if (!JDBCSchemaConnectionPanel.this.driverClass
								.getText().equals(driverClassName))
							JDBCSchemaConnectionPanel.this.driverClass
									.setText(driverClassName);
					}
			}
		});

		// Create a listener that listens for changes on the host, port
		// and database fields, and uses this to automatically update
		// and construct a JDBC URL based on their contents.
		this.host.getDocument().addDocumentListener(this);
		this.port.getDocument().addDocumentListener(this);
		this.database.getDocument().addDocumentListener(this);

		// Create a file chooser for finding the JAR file where the driver
		// lives.
		this.jarFileChooser = new JFileChooser();
		this.jarFileChooser.setFileFilter(new FileFilter() {
			// Accepts only files ending in ".jar".
			public boolean accept(final File f) {
				return f.isDirectory()
						|| f.getName().toLowerCase().endsWith(".jar");
			}

			public String getDescription() {
				return Resources.get("JARFileFilterDescription");
			}
		});

		// Add the driver class label and field.
		JLabel label = new JLabel(Resources.get("driverClassLabel"));
		gridBag.setConstraints(label, labelConstraints);
		this.add(label);
		JPanel field = new JPanel();
		field.add(this.predefinedDriverClass);
		field.add(this.driverClass);
		gridBag.setConstraints(field, fieldConstraints);
		this.add(field);

		// Add the driver location label, field and file chooser button.
		label = new JLabel(Resources.get("driverClassLocationLabel"));
		gridBag.setConstraints(label, labelConstraints);
		this.add(label);
		field = new JPanel();
		field.add(this.driverClassLocation);
		field.add(this.driverClassLocationButton);
		gridBag.setConstraints(field, fieldConstraints);
		this.add(field);

		// Add the host label, and the host field, port label, port field.
		label = new JLabel(Resources.get("hostLabel"));
		gridBag.setConstraints(label, labelConstraints);
		this.add(label);
		field = new JPanel();
		field.add(this.host);
		label = new JLabel(Resources.get("portLabel"));
		field.add(label);
		field.add(this.port);
		gridBag.setConstraints(field, fieldConstraints);
		this.add(field);

		// Add the database and schema fields.
		label = new JLabel(Resources.get("databaseLabel"));
		gridBag.setConstraints(label, labelConstraints);
		this.add(label);
		field = new JPanel();
		field.add(this.database);
		label = new JLabel(Resources.get("schemaNameLabel"));
		field.add(label);
		field.add(this.schemaName);
		gridBag.setConstraints(field, fieldConstraints);
		this.add(field);

		// Add the JDBC URL label and field.
		label = new JLabel(Resources.get("jdbcURLLabel"));
		gridBag.setConstraints(label, labelConstraints);
		this.add(label);
		field = new JPanel();
		field.add(this.jdbcURL);
		gridBag.setConstraints(field, fieldConstraints);
		this.add(field);

		// Add the username label, and the username field, password
		// label and password field across the username field space
		// in order to save space.
		label = new JLabel(Resources.get("usernameLabel"));
		gridBag.setConstraints(label, labelLastRowConstraints);
		this.add(label);
		field = new JPanel();
		field.add(this.username);
		label = new JLabel(Resources.get("passwordLabel"));
		field.add(label);
		field.add(this.password);
		gridBag.setConstraints(field, fieldLastRowConstraints);
		this.add(field);

		// Attach the file chooser to the driver class location button.
		final JPanel panel = this;
		this.driverClassLocationButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (JDBCSchemaConnectionPanel.this.jarFileChooser
						.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION) {
					final File file = JDBCSchemaConnectionPanel.this.jarFileChooser
							.getSelectedFile();
					// When a file is chosen, put its name in the driver
					// class location field.
					if (file != null)
						JDBCSchemaConnectionPanel.this.driverClassLocation
								.setText(file.toString());
				}
			}
		});
	}

	public void copySettingsFrom(final Properties template) {
		// If it is, copy everything over from it.
		this.driverClass.setText(template.getProperty("driverClass"));
		this.driverClassLocation.setText(template
				.getProperty("driverClassLocation"));

		// Make sure the right fields get enabled.
		this.driverClassChanged();

		// Carry on copying.
		final String jdbcURL = template.getProperty("jdbcURL");
		this.jdbcURL.setText(jdbcURL);
		this.username.setText(template.getProperty("username"));
		this.password.setText(template.getProperty("password"));
		this.schemaName.setText(template.getProperty("schema"));

		// Parse the JDBC URL into host, port and database, if the
		// driver is known to us (defined in the map at the start
		// of this class).
		String regexURL = this.currentJDBCURLTemplate;

		// Replace the three placeholders in the JDBC URL template
		// with regex patterns. Obviously, this depends on the
		// three placeholders appearing in the correct order.
		// If they don't, then you're stuffed.
		regexURL = regexURL.replaceAll("<HOST>", "(.*)");
		regexURL = regexURL.replaceAll("<PORT>", "(.*)");
		regexURL = regexURL.replaceAll("<DATABASE>", "(.*)");

		// Use the regex to parse out the host, port and database
		// from the JDBC URL.
		final Pattern regex = Pattern.compile(regexURL);
		final Matcher matcher = regex.matcher(jdbcURL);
		if (matcher.matches()) {
			this.host.setText(matcher.group(1));
			this.port.setText(matcher.group(2));
			this.database.setText(matcher.group(3));
		}
	}

	private void copySettingsFrom(final Schema template) {
		// Test to make sure the template is a JDBC Schema.
		if (template instanceof JDBCSchema) {
			final JDBCSchema jdbcSchema = (JDBCSchema) template;

			// If it is, copy everything over from it.
			this.driverClass.setText(jdbcSchema.getDriverClassName());
			this.driverClassLocation.setText(jdbcSchema
					.getDriverClassLocation() == null ? null : jdbcSchema
					.getDriverClassLocation().toString());

			// Make sure the right fields get enabled.
			this.driverClassChanged();

			// Carry on copying.
			final String jdbcURL = jdbcSchema.getJDBCURL();
			this.jdbcURL.setText(jdbcURL);
			this.username.setText(jdbcSchema.getUsername());
			this.password.setText(jdbcSchema.getPassword());
			this.schemaName.setText(jdbcSchema.getDatabaseSchema());

			// Parse the JDBC URL into host, port and database, if the
			// driver is known to us (defined in the map at the start
			// of this class).
			String regexURL = this.currentJDBCURLTemplate;

			// Replace the three placeholders in the JDBC URL template
			// with regex patterns. Obviously, this depends on the
			// three placeholders appearing in the correct order.
			// If they don't, then you're stuffed.
			regexURL = regexURL.replaceAll("<HOST>", "(.*)");
			regexURL = regexURL.replaceAll("<PORT>", "(.*)");
			regexURL = regexURL.replaceAll("<DATABASE>", "(.*)");

			// Use the regex to parse out the host, port and database
			// from the JDBC URL.
			final Pattern regex = Pattern.compile(regexURL);
			final Matcher matcher = regex.matcher(jdbcURL);
			if (matcher.matches()) {
				this.host.setText(matcher.group(1));
				this.port.setText(matcher.group(2));
				this.database.setText(matcher.group(3));
			}
		}
	}

	private void documentEvent(final DocumentEvent e) {
		if (e.getDocument().equals(this.driverClass.getDocument()))
			this.driverClassChanged();
		else
			this.updateJDBCURL();
	}

	private void driverClassChanged() {
		// Work out which class we should try out.
		final String className = this.driverClass.getText();

		// If it's not empty...
		if (!this.isEmpty(className)) {
			// Is this a preset class?
			for (final Iterator i = JDBCSchemaConnectionPanel.DRIVER_NAME_MAP
					.entrySet().iterator(); i.hasNext();) {
				final Map.Entry entry = (Map.Entry) i.next();
				final String mapName = (String) entry.getKey();
				final String mapClassName = (String) entry.getValue();
				if (mapClassName.equals(className))
					this.predefinedDriverClass.setSelectedItem(mapName);
			}

			// Do we know about this, as defined in the map at the start
			// of this class?
			if (JDBCSchemaConnectionPanel.DRIVER_MAP.containsKey(className)) {
				// Yes, so we can use the map to construct a JDBC URL
				// template, into which host, port, and database can be
				// placed as required.

				// Obtain the template and split it.
				final String[] parts = (String[]) JDBCSchemaConnectionPanel.DRIVER_MAP
						.get(className);

				// The second part is the JDBC URL template itself. Remember
				// which template was selected, then disable the JDBC URL
				// field in the interface as its contents will now be
				// computed automatically. Enable the host/database/port
				// fields instead.
				this.currentJDBCURLTemplate = parts[1];
				this.jdbcURL.setEnabled(false);
				this.host.setEnabled(true);
				this.port.setEnabled(true);
				this.database.setEnabled(true);

				// The first part of the template is the default port
				// number, so set the port field to that number.
				this.port.setText(parts[0]);
			}

			// This else statement deals with JDBC drivers that we do not
			// have a template for.
			else {
				// Blank out our current template, so that we don't try
				// and use it by accident.
				this.currentJDBCURLTemplate = null;

				// Enable the user-specified JDBC URL field, and disable
				// the host/port/database fields as they're no longer
				// required.
				this.jdbcURL.setEnabled(true);
				this.host.setEnabled(false);
				this.port.setEnabled(false);
				this.database.setEnabled(false);
			}

			// Attempt to load the driver class that the user specified.
			boolean classNotFound = true;
			try {
				Class.forName(className);
				classNotFound = false;
			} catch (final Throwable t) {
				classNotFound = true;
			}

			// If not found, then the user needs to specify a location
			// for the class too.
			this.driverClassLocation.setEnabled(classNotFound);
			this.driverClassLocationButton.setEnabled(classNotFound);
		}

		// If it's empty, disable the fields that depend on it.
		else {
			this.host.setEnabled(false);
			this.port.setEnabled(false);
			this.database.setEnabled(false);
			this.jdbcURL.setEnabled(false);
		}
	}

	public Class getSchemaClass() {
		return JDBCSchema.class;
	}
	
	private boolean isEmpty(final String string) {
		// Strings are empty if they are null or all whitespace.
		return string == null || string.trim().length() == 0;
	}

	private void updateJDBCURL() {
		// If we don't have a current template, we can't parse it,
		// so don't even attempt to do so.
		if (this.currentJDBCURLTemplate == null)
			return;

		// Update the JDBC URL based on our current settings. Do this
		// by replacing the placeholders in the template with the
		// current values of the host/port/database fields. If there
		// are no values in these fields, leave the placeholders
		// as they are.
		String newURL = this.currentJDBCURLTemplate;
		if (!this.isEmpty(this.host.getText()))
			newURL = newURL.replaceAll("<HOST>", this.host.getText());
		if (!this.isEmpty(this.port.getText()))
			newURL = newURL.replaceAll("<PORT>", this.port.getText());
		if (!this.isEmpty(this.database.getText()))
			newURL = newURL.replaceAll("<DATABASE>", this.database.getText());

		// Set the JDBC URL field to contain the URL we constructed.
		this.jdbcURL.setText(newURL);
	}

	public void changedUpdate(final DocumentEvent e) {
		this.documentEvent(e);
	}

	/**
	 * Creates a {@link JDBCSchema} with the given name, based on the user input
	 * inside the panel.
	 * 
	 * @param name
	 *            the name to give the schema.
	 * @return the created schema.
	 */
	public Schema createSchema(final String name) {
		// If the fields aren't valid, we can't create it.
		if (!this.validateFields())
			return null;

		try {
			// Record the user's specifications.
			final String driverClassName = this.driverClass.getText();
			final String driverClassLocation = this.driverClassLocation
					.getText();
			final String url = this.jdbcURL.getText();
			final String schemaName = this.schemaName.getText();
			final String username = this.username.getText();
			final String password = new String(this.password.getPassword());

			// Construct a JDBCSchema based on them.
			final JDBCSchema schema = MartBuilderUtils.createJDBCSchema(
					driverClassLocation == null ? null : new File(
							driverClassLocation), driverClassName, url,
					schemaName, username, password, name, false);

			// Return that schema.
			return schema;
		} catch (final Throwable t) {
			this.martTab.getMartTabSet().getMartBuilder().showStackTrace(t);
		}

		// If we got here, something went wrong, so behave
		// as though validation failed.
		return null;
	}

	public void insertUpdate(final DocumentEvent e) {
		this.documentEvent(e);
	}

	/**
	 * Updates the specified schema based on the user input.
	 * 
	 * @param schema
	 *            the schema to update.
	 * @return the updated schema, or null if it was not updated.
	 */
	public Schema modifySchema(final Schema schema) {
		// If the fields are not valid, we can't modify it.
		if (!this.validateFields())
			return null;

		// We can only update JDBCSchema objects.
		if (schema instanceof JDBCSchema)
			try {
				// Use the user input to update the fields on
				// the existing schema object.
				final JDBCSchema jschema = (JDBCSchema) schema;
				jschema.setDriverClassName(this.driverClass.getText());
				final String driverClassLocation = this.driverClassLocation
						.getText();
				jschema
						.setDriverClassLocation(driverClassLocation == null ? null
								: new File(driverClassLocation));
				jschema.setJDBCURL(this.jdbcURL.getText());
				jschema.setDatabaseSchema(this.schemaName.getText());
				jschema.setUsername(this.username.getText());
				jschema.setPassword(new String(this.password.getPassword()));

				// Update the settings in the history file.
				final Properties history = new Properties();
				history.setProperty("driverClass", this.driverClass.getText());
				history.setProperty("driverClassLocation",
						this.driverClassLocation.getText());
				history.setProperty("jdbcURL", this.jdbcURL.getText());
				history.setProperty("username", this.username.getText());
				history.setProperty("password", new String(this.password
						.getPassword()));
				history.setProperty("schema", this.schemaName.getText());
				SettingsCache.saveHistoryProperties(JDBCSchema.class, schema
						.getName(), history);
			} catch (final Throwable t) {
				this.martTab.getMartTabSet().getMartBuilder().showStackTrace(t);
			}

		// Return the modified schema, or the original schema if
		// it was not a JDBC schema.
		return schema;
	}

	public void removeUpdate(final DocumentEvent e) {
		this.documentEvent(e);
	}

	public void resetFields(final Schema template) {
		// Set the copy-settings box to nothing-selected.
		this.predefinedDriverClass.setSelectedIndex(-1);

		// If the template is a JDBC schema, copy the settings
		// from it.
		if (template instanceof JDBCSchema)
			this.copySettingsFrom(template);

		// Otherwise, set some sensible defaults.
		else {
			this.driverClass.setText(null);
			this.driverClassLocation.setText(null);

			// Make sure the right fields get enabled.
			this.driverClassChanged();

			// Carry on resetting.
			this.jdbcURL.setText(null);
			this.host.setText(null);
			this.port.setText(null);
			this.database.setText(null);
			this.schemaName.setText(null);
			this.username.setText(null);
			this.password.setText(null);
		}
	}

	/**
	 * Validates the fields. If any are invalid, it pops up a message saying so.
	 * 
	 * @return <tt>true</tt> if all is well, <tt>false</tt> if not.
	 */
	public boolean validateFields() {
		// Make a list to hold any validation messages that may occur.
		final List messages = new ArrayList();

		// If we don't have a class, complain.
		if (this.isEmpty(this.driverClass.getText()))
			messages.add(Resources.get("fieldIsEmpty", Resources
					.get("driverClass")));

		// If we do have a class, and we don't know where it lives, make
		// sure the user has told us where it lives.
		else if (this.driverClassLocation.isEnabled()
				&& this.isEmpty(this.driverClassLocation.getText()))
			messages.add(Resources.get("fieldIsEmpty", Resources
					.get("driverClassLocation")));

		// If the user had to specify their own JDBC URL, make sure
		// they have done so.
		if (this.jdbcURL.isEnabled()) {
			if (this.isEmpty(this.jdbcURL.getText()))
				messages.add(Resources.get("fieldIsEmpty", Resources
						.get("jdbcURL")));
		}

		// Otherwise, make sure they have specified all three of host, port
		// and database.
		else {
			if (this.isEmpty(this.host.getText()))
				messages.add(Resources.get("fieldIsEmpty", Resources
						.get("host")));
			if (this.isEmpty(this.port.getText()))
				messages.add(Resources.get("fieldIsEmpty", Resources
						.get("port")));
			if (this.isEmpty(this.database.getText()))
				messages.add(Resources.get("fieldIsEmpty", Resources
						.get("database")));
		}

		// Make sure they have given a schema name.
		if (this.isEmpty(this.schemaName.getText()))
			messages.add(Resources.get("fieldIsEmpty", Resources
					.get("schemaName")));

		// Make sure they have given a username. (Password is optional as
		// not all databases require one).
		if (this.isEmpty(this.username.getText()))
			messages.add(Resources.get("fieldIsEmpty", Resources
					.get("username")));

		// If there any messages to show the user, show them.
		if (!messages.isEmpty())
			JOptionPane.showMessageDialog(this,
					messages.toArray(new String[0]), Resources
							.get("validationTitle"),
					JOptionPane.INFORMATION_MESSAGE);

		// Validation succeeds if there were no messages.
		return messages.isEmpty();
	}
}
