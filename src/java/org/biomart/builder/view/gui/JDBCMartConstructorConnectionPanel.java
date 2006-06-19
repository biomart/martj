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

import org.biomart.builder.controller.JDBCMartConstructor;
import org.biomart.builder.controller.JDBCSchema;
import org.biomart.builder.controller.MartBuilderUtils;
import org.biomart.builder.controller.JDBCMartConstructor.JDBCMartConstructorType;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.MartConstructor;
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.SchemaGroup;
import org.biomart.builder.resources.BuilderBundle;

/**
 * This connection panel implementation allows a user to define some JDBC
 * connection parameters, such as hostname, username, driver class and if
 * necessary the location where the driver can be found. It uses this to
 * construct a JDBC URL, dynamically, and ultimately creates a
 * {@link JDBCMartConstructor} implementation which represents the connection.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.4, 19th June 2006
 * @since 0.1
 */
public class JDBCMartConstructorConnectionPanel extends
		MartConstructorConnectionPanel implements ActionListener {
	private static final long serialVersionUID = 1;

	// Please add any more default drivers that we support to this list. The
	// keys are the driver classnames, and the values are arrays of strings.
	// The first entry in the array should be the default port number for this
	// JDBC driver type, and the second entry should be an example JDBC URL.
	// Within the URL, the keywords <HOSTNAME>, <PORT> and <DATABASE> must all
	// appear in the order mentioned. Any other order will break the regex
	// replacement function elsewhere in this class.
	private static Map DRIVER_MAP = new HashMap();
	static {
		DRIVER_MAP.put("com.mysql.jdbc.Driver", new String[] { "3306",
				"jdbc:mysql://<HOST>:<PORT>/<DATABASE>" });
		DRIVER_MAP.put("oracle.jdbc.driver.OracleDriver", new String[] {
				"1531", "jdbc:oracle:thin:@<HOST>:<PORT>:<DATABASE>" });
		DRIVER_MAP.put("org.postgresql.Driver", new String[] { "5432",
				"jdbc:postgres://<HOST>:<PORT>/<DATABASE>" });
	}

	private String currentJDBCURLTemplate;

	private DataSetTabSet datasetTabSet;

	private JComboBox copysettings;

	private JComboBox type;

	private JFileChooser outputDDLFileChooser;

	private JComboBox driverClass;

	private JTextField driverClassLocation;

	private JButton driverClassLocationButton;

	private JTextField outputDDLLocation;

	private JButton outputDDLLocationButton;

	private JFileChooser jarFileChooser;

	private JTextField jdbcURL;

	private JTextField host;

	private JFormattedTextField port;

	private JTextField database;

	private JTextField username;

	private JPasswordField password;

	/**
	 * This constructor creates a panel with all the fields necessary to
	 * construct a {@link JDBCMartConstructor} instance, save the name which
	 * will be passed in elsewhere. It optionally takes a template which is used
	 * to populate the fields of the panel. If this template is null, then
	 * defaults are used instead.
	 * 
	 * @param datasetTabSet
	 *            the tabset we belong to.
	 * @param template
	 *            the template to use to fill the values in the panel with. If
	 *            this is null, defaults are used.
	 */
	public JDBCMartConstructorConnectionPanel(DataSetTabSet datasetTabSet,
			MartConstructor template) {
		super();

		// Remember our parents.
		this.datasetTabSet = datasetTabSet;

		// Create the layout manager for this panel.
		GridBagLayout gridBag = new GridBagLayout();
		this.setLayout(gridBag);

		// Create constraints for labels that are not in the last row.
		GridBagConstraints labelConstraints = new GridBagConstraints();
		labelConstraints.gridwidth = GridBagConstraints.RELATIVE;
		labelConstraints.fill = GridBagConstraints.HORIZONTAL;
		labelConstraints.anchor = GridBagConstraints.LINE_END;
		labelConstraints.insets = new Insets(0, 2, 0, 0);
		// Create constraints for fields that are not in the last row.
		GridBagConstraints fieldConstraints = new GridBagConstraints();
		fieldConstraints.gridwidth = GridBagConstraints.REMAINDER;
		fieldConstraints.fill = GridBagConstraints.NONE;
		fieldConstraints.anchor = GridBagConstraints.LINE_START;
		fieldConstraints.insets = new Insets(0, 1, 0, 2);
		// Create constraints for labels that are in the last row.
		GridBagConstraints labelLastRowConstraints = (GridBagConstraints) labelConstraints
				.clone();
		labelLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;
		// Create constraints for fields that are in the last row.
		GridBagConstraints fieldLastRowConstraints = (GridBagConstraints) fieldConstraints
				.clone();
		fieldLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;

		// Create all the useful fields in the dialog box.
		this.driverClassLocation = new JTextField(30);
		this.driverClassLocationButton = new JButton(BuilderBundle
				.getString("browseButton"));
		this.jdbcURL = new JTextField(40);
		this.host = new JTextField(10);
		this.port = new JFormattedTextField(new DecimalFormat("0"));
		this.port.setColumns(4);
		this.database = new JTextField(10);
		this.username = new JTextField(10);
		this.password = new JPasswordField(10);

		// The driver class box displays everything we know about by default,
		// as defined by the map at the start of this class.
		this.driverClass = new JComboBox(
				(String[]) JDBCMartConstructorConnectionPanel.DRIVER_MAP
						.keySet().toArray(new String[0]));
		this.driverClass.setEditable(true);
		this.driverClass.addActionListener(this);

		// Build a combo box that lists all other JDBCSchema instances in
		// the mart, and allows the user to copy settings from them.
		this.copysettings = new JComboBox();
		for (Iterator i = this.datasetTabSet.getMart().getDataSets().iterator(); i
				.hasNext();) {
			DataSet ds = (DataSet) i.next();
			if (ds.getMartConstructor() instanceof JDBCMartConstructor)
				this.copysettings.addItem(ds.getMartConstructor());
		}
		this.copysettings.addActionListener(this);

		// Create a listener that listens for changes on the host, port
		// and database fields, and uses this to automatically update
		// and construct a JDBC URL based on their contents.
		DocumentListener jdbcURLConstructor = new JDBCURLConstructor(this.host,
				this.port, this.database, this.jdbcURL);
		this.host.getDocument().addDocumentListener(jdbcURLConstructor);
		this.port.getDocument().addDocumentListener(jdbcURLConstructor);
		this.database.getDocument().addDocumentListener(jdbcURLConstructor);

		// Create a file chooser for finding the JAR file where the driver
		// lives.
		this.jarFileChooser = new JFileChooser();
		this.jarFileChooser.setFileFilter(new FileFilter() {
			// Accepts only files ending in ".jar".
			public boolean accept(File f) {
				return (f.isDirectory() || f.getName().toLowerCase().endsWith(
						".jar"));
			}

			public String getDescription() {
				return BuilderBundle.getString("JARFileFilterDescription");
			}
		});

		// Create the type combo box and add the listener.
		this.type = new JComboBox();
		this.type.addItem(JDBCMartConstructorType.EXTERNAL);
		this.type.addItem(JDBCMartConstructorType.INTERNAL);
		this.type.addItem(JDBCMartConstructorType.SINGLEFILE);
		this.type.addItem(JDBCMartConstructorType.MULTIFILE);
		this.type.addActionListener(this);

		// Create a file chooser for choosing the file to save the DDL to.
		this.outputDDLFileChooser = new JFileChooser();
		this.outputDDLFileChooser.setFileFilter(new FileFilter() {
			// Accepts .ddl and .sql files.
			public boolean accept(File f) {
				return (f.isDirectory()
						|| f.getName().toLowerCase().endsWith(".sql") || f
						.getName().toLowerCase().endsWith(".ddl"));
			}

			public String getDescription() {
				return BuilderBundle.getString("DDLFileFilterDescription");
			}
		});
		this.outputDDLLocation = new JTextField(30);
		this.outputDDLLocationButton = new JButton(BuilderBundle
				.getString("browseButton"));

		// Add the copy settings label and drop-down menu.
		JLabel label = new JLabel(BuilderBundle.getString("copySettingsLabel"));
		gridBag.setConstraints(label, labelConstraints);
		this.add(label);
		JPanel field = new JPanel();
		field.add(this.copysettings);
		gridBag.setConstraints(field, fieldConstraints);
		this.add(field);

		// Add the type and DDL file fields.
		label = new JLabel(BuilderBundle.getString("jdbcMCTypeLabel"));
		gridBag.setConstraints(label, labelConstraints);
		this.add(label);
		field = new JPanel();
		field.add(this.type);
		gridBag.setConstraints(field, fieldConstraints);
		this.add(field);
		label = new JLabel(BuilderBundle.getString("outputDDLLocationLabel"));
		gridBag.setConstraints(label, labelConstraints);
		this.add(label);
		field = new JPanel();
		field.add(this.outputDDLLocation);
		field.add(this.outputDDLLocationButton);
		gridBag.setConstraints(field, fieldConstraints);
		this.add(field);

		// Add the driver class label and field.
		label = new JLabel(BuilderBundle.getString("driverClassLabel"));
		gridBag.setConstraints(label, labelConstraints);
		this.add(label);
		field = new JPanel();
		field.add(this.driverClass);
		gridBag.setConstraints(field, fieldConstraints);
		this.add(field);

		// Add the driver location label, field and file chooser button.
		label = new JLabel(BuilderBundle.getString("driverClassLocationLabel"));
		gridBag.setConstraints(label, labelConstraints);
		this.add(label);
		field = new JPanel();
		field.add(this.driverClassLocation);
		field.add(this.driverClassLocationButton);
		gridBag.setConstraints(field, fieldConstraints);
		this.add(field);

		// Add the host label, and the host field, port label, port field,
		// database label, and database field in the space where the host
		// field would normally go, to save space.
		label = new JLabel(BuilderBundle.getString("hostLabel"));
		gridBag.setConstraints(label, labelConstraints);
		this.add(label);
		field = new JPanel();
		field.add(this.host);
		label = new JLabel(BuilderBundle.getString("portLabel"));
		field.add(label);
		field.add(this.port);
		label = new JLabel(BuilderBundle.getString("databaseLabel"));
		field.add(label);
		field.add(this.database);
		gridBag.setConstraints(field, fieldConstraints);
		this.add(field);

		// Add the JDBC URL label and field.
		label = new JLabel(BuilderBundle.getString("jdbcURLLabel"));
		gridBag.setConstraints(label, labelConstraints);
		this.add(label);
		field = new JPanel();
		field.add(this.jdbcURL);
		gridBag.setConstraints(field, fieldConstraints);
		this.add(field);

		// Add the username label, and the username field, password
		// label and password field across the username field space
		// in order to save space.
		label = new JLabel(BuilderBundle.getString("usernameLabel"));
		gridBag.setConstraints(label, labelLastRowConstraints);
		this.add(label);
		field = new JPanel();
		field.add(this.username);
		label = new JLabel(BuilderBundle.getString("passwordLabel"));
		field.add(label);
		field.add(this.password);
		gridBag.setConstraints(field, fieldLastRowConstraints);
		this.add(field);

		// Attach the file chooser to the driver class location button.
		final JPanel panel = this;
		this.driverClassLocationButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (jarFileChooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION) {
					File file = jarFileChooser.getSelectedFile();
					// When a file is chosen, put its name in the driver
					// class location field.
					if (file != null)
						driverClassLocation.setText(file.toString());
				}
			}
		});

		// Attach the file chooser to the output DDL location button.
		this.outputDDLLocationButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (outputDDLFileChooser.showSaveDialog(panel) == JFileChooser.APPROVE_OPTION) {
					File file = outputDDLFileChooser.getSelectedFile();
					// When a file is chosen, put its name in the output
					// DDL location field.
					if (file != null)
						outputDDLLocation.setText(file.toString());
				}
			}
		});

		// Reset the fields to their defaults, based on the
		// template provided (if any).
		this.resetFields(template);
	}

	private void resetFields(MartConstructor template) {
		// Set the copy-settings box to nothing-selected.
		this.copysettings.setSelectedIndex(-1);

		// If the template is a JDBC one, copy the settings
		// from it.
		if (template instanceof JDBCMartConstructor)
			this.copySettingsFrom(template);

		// Otherwise, set some sensible defaults.
		else {
			this.driverClass.setSelectedIndex(-1);
			this.driverClassLocation.setText(null);
			this.jdbcURL.setText(null);
			this.host.setText(null);
			this.port.setText(null);
			this.database.setText(null);
			this.username.setText(null);
			this.password.setText(null);
			this.outputDDLLocation.setText(null);
			this.type.setSelectedIndex(-1);
		}
	}

	private void copySettingsFrom(MartConstructor template) {
		// Test to make sure the template is a JDBC one.
		if (template instanceof JDBCMartConstructor) {
			JDBCMartConstructor jdbcMartConstructor = (JDBCMartConstructor) template;

			// If it is, copy everything over from it.
			this.driverClass.setSelectedItem(jdbcMartConstructor
					.getDriverClassName());
			this.driverClassLocation.setText(jdbcMartConstructor
					.getDriverClassLocation() == null ? null
					: jdbcMartConstructor.getDriverClassLocation().toString());
			String jdbcURL = jdbcMartConstructor.getJDBCURL();
			this.jdbcURL.setText(jdbcURL);
			this.username.setText(jdbcMartConstructor.getUsername());
			this.password.setText(jdbcMartConstructor.getPassword());
			this.outputDDLLocation.setText(jdbcMartConstructor
					.getOutputDDLZipFile() == null ? null : jdbcMartConstructor
					.getOutputDDLZipFile().toString());
			this.type.setSelectedItem(jdbcMartConstructor.getType());

			// Parse the JDBC URL into host, port and database, if the
			// driver is known to us (defined in the map at the start
			// of this class).
			String regexURL = new String(this.currentJDBCURLTemplate);

			// Replace the three placeholders in the JDBC URL template
			// with regex patterns. Obviously, this depends on the
			// three placeholders appearing in the correct order.
			// If they don't, then you're stuffed.
			regexURL = regexURL.replaceAll("<HOST>", "(.*)");
			regexURL = regexURL.replaceAll("<PORT>", "(.*)");
			regexURL = regexURL.replaceAll("<DATABASE>", "(.*)");

			// Use the regex to parse out the host, port and database
			// from the JDBC URL.
			Pattern regex = Pattern.compile(regexURL);
			Matcher matcher = regex.matcher(jdbcURL);
			if (matcher.matches()) {
				this.host.setText(matcher.group(1));
				this.port.setText(matcher.group(2));
				this.database.setText(matcher.group(3));
			}
		}
	}

	/**
	 * Validates the fields. If any are invalid, it pops up a message saying so.
	 * 
	 * @return <tt>true</tt> if all is well, <tt>false</tt> if not.
	 */
	public boolean validateFields() {
		// Make a list to hold any validation messages that may occur.
		List messages = new ArrayList();

		// Make sure a type is selected.
		if (this.type.getSelectedIndex() == -1)
			messages.add(BuilderBundle.getString("fieldIsEmpty", BuilderBundle
					.getString("jdbcMCType")));
		// If FILE is selected, make sure a file is specified.
		else if ((this.type.getSelectedItem().equals(
				JDBCMartConstructorType.MULTIFILE) || this.type
				.getSelectedItem().equals(JDBCMartConstructorType.SINGLEFILE))
				&& this.isEmpty(this.outputDDLLocation.getText()))
			messages.add(BuilderBundle.getString("fieldIsEmpty", BuilderBundle
					.getString("outputDDLLocation")));

		// If we don't have a class, complain.
		if (this.isEmpty((String) this.driverClass.getSelectedItem()))
			messages.add(BuilderBundle.getString("fieldIsEmpty", BuilderBundle
					.getString("driverClass")));

		// If we do have a class, and we don't know where it lives, make
		// sure the user has told us where it lives.
		else if (this.driverClassLocation.isEnabled()
				&& this.isEmpty(this.driverClassLocation.getText()))
			messages.add(BuilderBundle.getString("fieldIsEmpty", BuilderBundle
					.getString("driverClassLocation")));

		// If the user had to specify their own JDBC URL, make sure
		// they have done so.
		if (this.jdbcURL.isEnabled()) {
			if (this.isEmpty(this.jdbcURL.getText()))
				messages.add(BuilderBundle.getString("fieldIsEmpty",
						BuilderBundle.getString("jdbcURL")));
		}

		// Otherwise, make sure they have specified all three of host, port
		// and database.
		else {
			if (this.isEmpty(this.host.getText()))
				messages.add(BuilderBundle.getString("fieldIsEmpty",
						BuilderBundle.getString("host")));
			if (this.isEmpty(this.port.getText()))
				messages.add(BuilderBundle.getString("fieldIsEmpty",
						BuilderBundle.getString("port")));
			if (this.isEmpty(this.database.getText()))
				messages.add(BuilderBundle.getString("fieldIsEmpty",
						BuilderBundle.getString("database")));
		}

		// Make sure they have given a username. (Password is optional as
		// not all databases require one).
		if (this.isEmpty(this.username.getText()))
			messages.add(BuilderBundle.getString("fieldIsEmpty", BuilderBundle
					.getString("username")));

		// If there any messages to show the user, show them.
		if (!messages.isEmpty())
			JOptionPane.showMessageDialog(this,
					messages.toArray(new String[0]), BuilderBundle
							.getString("validationTitle"),
					JOptionPane.INFORMATION_MESSAGE);

		// Validation succeeds if there were no messages.
		return messages.isEmpty();
	}

	private boolean isEmpty(String string) {
		// Strings are empty if they are null or all whitespace.
		return (string == null || string.trim().length() == 0);
	}

	public void actionPerformed(ActionEvent e) {
		// This method is called when the driver class field is
		// changed, either by the user typing in it, or using
		// the drop-down to select a predefine value.

		// It is also called when the user selects an existing
		// mart constructor from the copy-settings box.

		// Copy settings box selected?
		if (e.getSource() == this.copysettings) {
			// Identify which mart constructor to copy settings from.
			Object obj = this.copysettings.getSelectedItem();

			// If one was actually seleted, copy the settings from it.
			if (obj != null)
				this.copySettingsFrom((MartConstructor) obj);
		}

		// Type box selected?
		else if (e.getSource() == this.type) {
			// Identify which type was chosen.
			Object obj = this.type.getSelectedItem();

			// If one was actually seleted, check to see
			// if we need to enable/disable the file field.
			if (obj != null
					&& (obj.equals(JDBCMartConstructorType.MULTIFILE) || obj
							.equals(JDBCMartConstructorType.SINGLEFILE))) {
				// It is, so enable the fields.
				this.outputDDLLocation.setEnabled(true);
				this.outputDDLLocationButton.setEnabled(true);
			} else {
				// It isn't, do disable and blank the fields.
				this.outputDDLLocation.setEnabled(false);
				this.outputDDLLocationButton.setEnabled(false);
				this.outputDDLLocation.setText(null);
			}

			// If not external, copy JDBC settings from source schema,
			// except for target database name which can be left untouched.
			if (obj != null && !obj.equals(JDBCMartConstructorType.EXTERNAL)) {
				// Work out source schema.
				Schema schema = this.datasetTabSet.getSelectedDataSetTab()
						.getDataSet().getCentralTable().getSchema();
				if (schema instanceof SchemaGroup)
					schema = (Schema) ((SchemaGroup) schema).getSchemas()
							.iterator().next();

				// If it's a JDBC schema, copy the settings.
				if (schema instanceof JDBCSchema) {
					JDBCSchema jdbcSchema = (JDBCSchema) schema;
					this.driverClass.setSelectedItem(jdbcSchema
							.getDriverClassName());
					this.driverClassLocation.setText(jdbcSchema
							.getDriverClassLocation() == null ? null
							: jdbcSchema.getDriverClassLocation().toString());
					String jdbcURL = jdbcSchema.getJDBCURL();
					this.jdbcURL.setText(jdbcURL);
					this.username.setText(jdbcSchema.getUsername());
					this.password.setText(jdbcSchema.getPassword());

					// Parse the JDBC URL into host, port and database, if the
					// driver is known to us (defined in the map at the start
					// of this class).
					String regexURL = new String(this.currentJDBCURLTemplate);

					// Replace the three placeholders in the JDBC URL template
					// with regex patterns. Obviously, this depends on the
					// three placeholders appearing in the correct order.
					// If they don't, then you're stuffed.
					regexURL = regexURL.replaceAll("<HOST>", "(.*)");
					regexURL = regexURL.replaceAll("<PORT>", "(.*)");
					regexURL = regexURL.replaceAll("<DATABASE>", "(.*)");

					// Use the regex to parse out the host, port and database
					// from the JDBC URL.
					Pattern regex = Pattern.compile(regexURL);
					Matcher matcher = regex.matcher(jdbcURL);
					if (matcher.matches()) {
						this.host.setText(matcher.group(1));
						this.port.setText(matcher.group(2));
						// NOT the database, as the user will override this.
					}
				}
			}
		}

		// Driver class field changed?
		else if (e.getSource() == this.driverClass) {
			// Work out which class we should try out.
			String className = (String) this.driverClass.getSelectedItem();

			// If one has actually been specified...
			if (!this.isEmpty(className)) {
				// ...then we should try and work out the default settings for
				// it.

				// Do we know about this, as defined in the map at the start
				// of this class?
				if (JDBCMartConstructorConnectionPanel.DRIVER_MAP
						.containsKey(className)) {
					// Yes, so we can use the map to construct a JDBC URL
					// template, into which host, port, and database can be
					// placed as required.

					// Obtain the template and split it.
					String[] parts = (String[]) JDBCMartConstructorConnectionPanel.DRIVER_MAP
							.get(className);

					// The first part of the template is the default port
					// number, so set the port field to that number.
					this.port.setText(parts[0]);

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

					// Work out what we're changing from and to.
					if (this.jdbcURL.isEnabled()) {
						// We're changing from custom to predefined. This
						// means we need to blank out any existing JDBC URL,
						// host, and database information (the port has
						// already been set).
						this.jdbcURL.setText(null);
						this.host.setText(null);
						this.database.setText(null);
					} else {
						// We're changing from predefined to another predefined.
						// No need to do anything.
					}
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

					// Work out what we're changing from and to.
					if (this.jdbcURL.isEnabled()) {
						// We're changing from custom to another custom.
						// No need to take any action.
					} else {
						// We're changing from predefined to custom, so
						// we need to blank out the host/port/database fields
						// so they don't interfere.
						this.jdbcURL.setText(null);
						this.host.setText(null);
						this.port.setText(null);
						this.database.setText(null);
					}
				}

				// Attempt to load the driver class that the user specified.
				boolean classNotFound = true;
				try {
					Class.forName(className);
					classNotFound = false;
				} catch (Throwable t) {
					classNotFound = true;
				}

				// If not found, then the user needs to specify a location
				// for the class too.
				this.driverClassLocation.setEnabled(classNotFound);
			}

			// No driver selected? Grey out all the bits that depend
			// on the driver being selected.
			else {
				this.jdbcURL.setEnabled(false);
				this.host.setEnabled(false);
				this.port.setEnabled(false);
				this.database.setEnabled(false);
				this.driverClassLocation.setEnabled(false);
			}
		}
	}

	/**
	 * Creates a {@link JDBCMartConstructor} with the given name, based on the
	 * user input inside the panel.
	 * 
	 * @param name
	 *            the name to give the mart constructor.
	 * @return the created mart constructor.
	 */
	public MartConstructor createMartConstructor(String name) {
		// If the fields aren't valid, we can't create it.
		if (!this.validateFields())
			return null;

		try {
			// Record the user's specifications.
			String driverClassName = (String) this.driverClass
					.getSelectedItem();
			String driverClassLocation = this.driverClassLocation.getText();
			String url = this.jdbcURL.getText();
			String username = this.username.getText();
			String password = new String(this.password.getPassword());
			String outputDDLLocation = this.outputDDLLocation.getText();
			JDBCMartConstructorType type = (JDBCMartConstructorType) this.type
					.getSelectedItem();

			// Construct a JDBCMartConstructor based on them.
			JDBCMartConstructor mc = MartBuilderUtils
					.createJDBCMartConstructor(
							driverClassLocation == null ? null : new File(
									driverClassLocation), driverClassName, url,
							username, password, name, type,
							outputDDLLocation == null ? null : new File(
									outputDDLLocation));

			// Return that mart constructor.
			return mc;
		} catch (Throwable t) {
			this.datasetTabSet.getMartTabSet().getMartBuilder().showStackTrace(
					t);
		}

		// If we got here, something went wrong, so behave
		// as though validation failed.
		return null;
	}

	// This class updates the JDBC URL based on the user input in the
	// host, port and database fields, and the template for the currently
	// selected driver class.
	private class JDBCURLConstructor implements DocumentListener {
		private JTextField host;

		private JTextField port;

		private JTextField database;

		private JTextField jdbcURL;

		/**
		 * This constructor creates a listener, with references to the fields
		 * that it is listening for changes on, and the field to construct the
		 * JDBC URL in.
		 * 
		 * @param host
		 *            the host field to listen to.
		 * @param port
		 *            the port field to listen to.
		 * @param database
		 *            the database field to listen to.
		 * @param jdbcURL
		 *            the JDBC URL field to place the constructed URL in.
		 */
		public JDBCURLConstructor(JTextField host, JTextField port,
				JTextField database, JTextField jdbcURL) {
			this.host = host;
			this.port = port;
			this.database = database;
			this.jdbcURL = jdbcURL;
		}

		public void insertUpdate(DocumentEvent e) {
			this.updateJDBCURL();
		}

		public void removeUpdate(DocumentEvent e) {
			this.updateJDBCURL();
		}

		public void changedUpdate(DocumentEvent e) {
			this.updateJDBCURL();
		}

		private void updateJDBCURL() {
			// If we don't have a current template, we can't parse it,
			// so don't even attempt to do so.
			if (currentJDBCURLTemplate == null)
				return;

			// Update the JDBC URL based on our current settings. Do this
			// by replacing the placeholders in the template with the
			// current values of the host/port/database fields. If there
			// are no values in these fields, leave the placeholders
			// as they are.
			String newURL = new String(currentJDBCURLTemplate);
			if (!isEmpty(this.host.getText()))
				newURL = newURL.replaceAll("<HOST>", this.host.getText());
			if (!isEmpty(this.port.getText()))
				newURL = newURL.replaceAll("<PORT>", this.port.getText());
			if (!isEmpty(this.database.getText()))
				newURL = newURL.replaceAll("<DATABASE>", this.database
						.getText());

			// Set the JDBC URL field to contain the URL we constructed.
			this.jdbcURL.setText(newURL);
		}
	}
}
