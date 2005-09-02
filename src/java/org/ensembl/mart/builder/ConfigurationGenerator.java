/*
 * Created on Sep 1, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.ensembl.mart.builder;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.ensembl.mart.builder.lib.Column;
import org.ensembl.mart.builder.lib.DatabaseAdaptor;
import org.ensembl.mart.builder.lib.Dataset;
import org.ensembl.mart.builder.lib.MetaDataResolver;
import org.ensembl.mart.builder.lib.Table;
import org.ensembl.mart.builder.lib.Transformation;
import org.ensembl.mart.builder.lib.TransformationConfig;
import org.ensembl.mart.builder.lib.TransformationUnit;

/**
 * @author damian@ebi.ac.uk
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ConfigurationGenerator {

	private HashMap tableList = new HashMap();
	private HashMap refColNames = new HashMap();
	private HashMap refColAliases = new HashMap();
	private HashMap cardinalityFirst = new HashMap();
	private HashMap cardinalitySecond = new HashMap();
	private String cenColName = "";
	private String cenColAlias = "";
	private String userTableName;
	private String centralExtensionColumn;
	private String centralExtensionCondition;
	private String partitionExtension;
	private int leftJoin = 0;
	private String[] dialogOptions =
		new String[] { "Continue", "Select columns", "Finish" };
	private String[] standardOptions = new String[] { "OK", "Cancel" };
	private String[] includeCentralFilterOptions = new String[] { "N", "Y" };

	private MetaDataResolver resolver;
	private String schema;
	private DatabaseAdaptor adaptor;

	public ConfigurationGenerator() {
		resolver = MartBuilder.getResolver();
		adaptor = MartBuilder.getAdaptor();
	}

	public TransformationConfig createConfig() {
		// TRANSFORMATION CONFIG SETTINGS		
		String tConfigName =
			JOptionPane.showInputDialog("Transformation config name", "test");
		if (tConfigName == null)
			return new TransformationConfig("");
		TransformationConfig tConfig = new TransformationConfig(tConfigName);

		// cycle through adding datasets to this transformation config
		int datasetContinueOption = 0;
		while (datasetContinueOption != 1) {
			int transformationCount = 0;
			// DATASET SETTINGS
			String datasetName =
				JOptionPane.showInputDialog("Dataset name", "test");
			if (datasetName == null)
				return tConfig;
			Dataset dataset = new Dataset(datasetName, "");

			// MAIN TABLE - NAME AND PARTITIONING SETTINGS 
			String[] potentialTables = resolver.getAllTableNames();
			Box centralSettings = new Box(BoxLayout.Y_AXIS);
			centralSettings.add(Box.createRigidArea(new Dimension(400, 1)));
			Box box1 = new Box(BoxLayout.X_AXIS);
			JLabel label1 = new JLabel("Main table name");
			box1.add(label1);
			JComboBox tableNameBox = new JComboBox(potentialTables);
			box1.add(tableNameBox);
			centralSettings.add(box1);
			JCheckBox partitionBox = new JCheckBox("Partition into datasets");
			centralSettings.add(partitionBox);
			int option2 =
				JOptionPane.showOptionDialog(
					null,
					centralSettings,
					"Main Table Settings",
					JOptionPane.DEFAULT_OPTION,
					JOptionPane.PLAIN_MESSAGE,
					null,
					dialogOptions,
					null);

			String centralTableName = tableNameBox.getSelectedItem().toString();
			if (option2 == 2)
				break;
			else if (option2 == 1)
				chooseColumns(centralTableName);

			partitionExtension = "";
			centralExtensionColumn = "";
			centralExtensionCondition = "";
			userTableName = "";

			if (partitionBox.getSelectedObjects() == null) {
				// MAIN TABLE - USER TABLE AND PROJECTION/RESTRICTION SETTINGS IF NOT PARTITIONING
				String tableType = "m";
				chooseCentralTableSettings(centralTableName, datasetName, tableType);
				// CREATE THE TRANSFORMATION
				dataset.getElement().setAttribute("mainTable", centralTableName);
				Integer tCount = new Integer(transformationCount + 1);
				Transformation transformation =
					new Transformation(
						tCount.toString(),
						tableType,
						centralTableName,
						userTableName,
						"N");
				Table[] referencedTables =
					resolver.getReferencedTables(centralTableName);
				transformation =
					generateTransformation(
						referencedTables,
						centralTableName,
						tableType,
						datasetName,
						"",
						1,
						leftJoin,
						transformation);
				dataset.insertChildObject(transformationCount, transformation);
				transformationCount++;
			} else {
				// MAIN TABLE - DO DATASET PARTITIONING
				// NOT IMPLEMENTED YET

				/*ArrayList allCols = new ArrayList();
				Table[] referencedTables = resolver.getReferencedTables(centralTableName);
				int centralSeen = 0;
				for (int k = 0; k < referencedTables.length; k++){
					if (centralSeen > 0 && referencedTables[k].getName().equals(centralTableName))
					  continue;// stops repetition of central Table cols
					Column[] tableCols = referencedTables[k].getColumns();
					for (int l = 0; l < tableCols.length; l++){
						String entry = referencedTables[k].getName()+"."+tableCols[l].getName();
						allCols.add(entry);
					}
					if (referencedTables[k].getName().equals(centralTableName))
					  centralSeen++;
				}
				String[] cols = new String[allCols.size()];
				allCols.toArray(cols);
				String colsOption = (String) JOptionPane.showInputDialog(null,"Choose column to partition on",
						"",JOptionPane.PLAIN_MESSAGE,null,cols,"");			 
				String[] chosenOptions = colsOption.split("\\.+");
				String chosenTable = chosenOptions[0];
				String chosenColumn = chosenOptions[1];
				
				// moved sql to resolver 
				ArrayList allValList=resolver.getDistinctValuesForPartitioning(chosenColumn,chosenTable);
				
				String[] values;
				if (allValList.size() > 20){
					  String userValues = JOptionPane.showInputDialog(null,"Too many values to display - " +
						  "enter comma separated list");
					  String[] indValues = userValues.split(",");
					  values = new String[indValues.length];
					  for (int i = 0; i < indValues.length; i++){
						  values[i] = chosenColumn+"="+indValues[i];	
					  }
				}
				else{
					  Box colOps = new Box(BoxLayout.Y_AXIS);
					  JCheckBox[] checks = new JCheckBox[allValList.size()];
					  for (int i = 0; i < allValList.size(); i++){
						  checks[i] = new JCheckBox((String) allValList.get(i));
						  checks[i].setSelected(true);
						  colOps.add(checks[i]);	  
					  }
					  int valsOption = JOptionPane.showOptionDialog(null,colOps,"Select values for partitioning ",
								   JOptionPane.DEFAULT_OPTION,JOptionPane.PLAIN_MESSAGE,null,standardOptions,null);	
					  ArrayList valueList = new ArrayList();	
					  for (int i = 0; i < allValList.size(); i++){
						  if (checks[i].getSelectedObjects() == null)
							  continue;	
							  String partitionExtension = chosenColumn+"="+checks[i].getText();
							  valueList.add(partitionExtension);
					  }
					  values = new String[valueList.size()];
					  valueList.toArray(values);
				}
				//String mainTablePartition = (String) JOptionPane.showInputDialog(null,
				 //"Partition value to use for the main table","",
				//		JOptionPane.PLAIN_MESSAGE,null,values,null);
						
				int manualChoose = JOptionPane.showConfirmDialog(null,"Autogenerate each transformation");
				
				String includeCentral = (String) JOptionPane.showInputDialog(null,
				"Include central filters for dm tables?","",
									 JOptionPane.PLAIN_MESSAGE,null,includeCentralFilterOptions,null);
						
				for (int i = 0; i < values.length;i++){// loop through each partition type	  
					String partitionExtension = values[i];	
					if (chosenTable.equals(centralTableName)){
					   // set centralExtension
					   centralExtension = partitionExtension;
					   centralExtensionColumn = centralExtension.split("=")[0];
					   centralExtensionCondition = "="+centralExtension.split("=")[1];		  	 
					}	
					String tableType,includeCentralFilter;
					//if (partitionExtension.equals(mainTablePartition)){	
					  userTableName = (datasetName+"__"+partitionExtension.split("=")[1]+"__"+"main").toLowerCase();	
					  tableType = "m";
					  includeCentralFilter = "N";
					  dataset.getElement().setAttribute("mainTable",centralTableName); 	
					//}
					//else{
				 //	userTableName = (datasetName+"__"+partitionExtension.split("=")[1]+"__"+"dm").toLowerCase();	
				 //	tableType = "dm";
				 //	includeCentralFilter = includeCentral;
					//}
					Integer tCount = new Integer(transformationCount+1);
					//cenColName = "";
					//cenColAlias = "";
					Transformation transformation = new Transformation();
					transformation.getElement().setAttribute("internalName",tCount.toString());
					transformation.getElement().setAttribute("tableType",tableType);
					transformation.getElement().setAttribute("centralTable",centralTableName);
					transformation.getElement().setAttribute("userTableName",userTableName);
					transformation.getElement().setAttribute("includeCentralFilter",includeCentralFilter);
					//not for main table T
				 
					//String[] columnNames = {"%"};	
					referencedTables = resolver.getReferencedTables(centralTableName);
				 
					System.out.println(centralExtension+":"+centralExtensionColumn+":"+centralExtensionCondition);
				 
					if (i == 0)
					  transformation = getCardinalities(referencedTables, centralTableName, tableType, datasetName,
					   centralExtension,centralExtensionColumn, centralExtensionCondition, transformationCount,
					    chosenTable, partitionExtension, 1, transformation);
					else
					  transformation = getCardinalities(referencedTables, centralTableName, tableType, datasetName,
					   centralExtension,
						  centralExtensionColumn, centralExtensionCondition,transformationCount, 
						  chosenTable, partitionExtension, manualChoose, transformation);
				
					dataset.insertChildObject(transformationCount,transformation);	
					transformationCount++;
				
					tableList.remove(centralTableName);
				
					potentialTables = new String[tableList.size()];	
					tableList.keySet().toArray(potentialTables);
				 } // end of loop */
			}

			// DIMENSION TABLE TRANSFORMATIONS			
			potentialTables = new String[tableList.size()];
			tableList.keySet().toArray(potentialTables);
			while (centralTableName != null
				&& tableList.size() > 0) { // loop thro while still got candidates
				// DIMENSION TABLE - NAME, INCLUDE CENTRAL AND PARTITIOING SETTINGS	
				leftJoin = 0;
				partitionExtension = "";
				centralExtensionColumn = "";
				centralExtensionCondition = "";
				cenColName = "";
				cenColAlias = "";

				centralSettings = new Box(BoxLayout.Y_AXIS);
				centralSettings.add(Box.createRigidArea(new Dimension(400, 1)));
				box1 = new Box(BoxLayout.X_AXIS);
				Box box2 = new Box(BoxLayout.X_AXIS);
				Box box3 = new Box(BoxLayout.X_AXIS);
				label1 = new JLabel("Central table name");
				box1.add(label1);
				tableNameBox = new JComboBox(potentialTables);
				box1.add(tableNameBox);
				centralSettings.add(box1);
				JLabel label3 = new JLabel("Include central filters?");
				box3.add(label3);
				JComboBox includeCentralBox =
					new JComboBox(includeCentralFilterOptions);
				box3.add(includeCentralBox);
				centralSettings.add(box3);
				partitionBox = new JCheckBox("Use partitioning?");
				centralSettings.add(partitionBox);
				JCheckBox leftJoinBox = new JCheckBox("Use left join?");
				centralSettings.add(leftJoinBox);
				int option3 =
					JOptionPane.showOptionDialog(
						null,
						centralSettings,
						"Central Table Settings",
						JOptionPane.DEFAULT_OPTION,
						JOptionPane.PLAIN_MESSAGE,
						null,
						dialogOptions,
						null);

				centralTableName = tableNameBox.getSelectedItem().toString();
				String includeCentralFilters =
					includeCentralBox.getSelectedItem().toString();

				if (option3 == 2)
					break;
				if (option3 == 1)
					chooseColumns(centralTableName);

				if (leftJoinBox.getSelectedObjects() != null)
					leftJoin = 1;

				if (partitionBox.getSelectedObjects() == null) {
					// DIMENSION TABLE - NON PARTITIONED
					String tableType = "d";
					chooseCentralTableSettings(
						centralTableName,
						datasetName,
						tableType);
					//	CREATE THE TRANSFORMATION 		  
					Integer tCount = new Integer(transformationCount + 1);
					Transformation transformation =
						new Transformation(
							tCount.toString(),
							tableType,
							centralTableName,
							userTableName,
							includeCentralFilters);

					Table[] referencedTables =
						resolver.getReferencedTables(centralTableName);
					transformation =
						generateTransformation(
							referencedTables,
							centralTableName,
							tableType,
							datasetName,
							"",
							1,
							leftJoin,
							transformation);

					dataset.insertChildObject(
						transformationCount,
						transformation);
					transformationCount++;

					tableList.remove(centralTableName);
					potentialTables = new String[tableList.size()];
					tableList.keySet().toArray(potentialTables);
				} else {
					// DIMENSION TABLE - DO PARTITIONING
					ArrayList allCols = new ArrayList();
					Table[] referencedTables =
						resolver.getReferencedTables(centralTableName);
					int centralSeen = 0;
					for (int k = 0; k < referencedTables.length; k++) {
						if (centralSeen > 0
							&& referencedTables[k].getName().equals(centralTableName))
							continue;
						Column[] tableCols = referencedTables[k].getColumns();
						for (int l = 0; l < tableCols.length; l++) {
							String entry =
								referencedTables[k].getName()
									+ "."
									+ tableCols[l].getName();
							allCols.add(entry);
						}
						if (referencedTables[k].getName().equals(centralTableName))
							centralSeen++;
					}
					String[] cols = new String[allCols.size()];
					allCols.toArray(cols);
					String colsOption =
						(String) JOptionPane.showInputDialog(
							null,
							"Choose column to partition on",
							"",
							JOptionPane.PLAIN_MESSAGE,
							null,
							cols,
							"");
					String[] chosenOptions = colsOption.split("\\.+");
					String chosenTable = chosenOptions[0];
					String chosenColumn = chosenOptions[1];

					ArrayList allValList =
						resolver.getDistinctValuesForPartitioning(
							chosenColumn,
							chosenTable);

					String[] values;
												
					if (allValList.size() > 20 || allValList.size() == 0) {// hack for empty tables during dev
						String userValues =
							JOptionPane.showInputDialog(
								null,
								"Too many values to display - "
									+ "enter comma separated list");
						String[] indValues = userValues.split(",");
						values = new String[indValues.length];
						for (int i = 0; i < indValues.length; i++) {
							values[i] = chosenColumn + "=" + indValues[i];
						}
					} else {
						Box colOps = new Box(BoxLayout.Y_AXIS);
						JCheckBox[] checks = new JCheckBox[allValList.size()];
						for (int i = 0; i < allValList.size(); i++) {
							checks[i] =
								new JCheckBox((String) allValList.get(i));
							checks[i].setSelected(true);
							colOps.add(checks[i]);
						}
						int valsOption =
							JOptionPane.showOptionDialog(
								null,
								colOps,
								"Select values for partitioning ",
								JOptionPane.DEFAULT_OPTION,
								JOptionPane.PLAIN_MESSAGE,
								null,
								standardOptions,
								null);
						ArrayList valueList = new ArrayList();
						for (int i = 0; i < allValList.size(); i++) {
							if (checks[i].getSelectedObjects() == null)
								continue;
							partitionExtension =
								chosenColumn + "=" + checks[i].getText();
							valueList.add(partitionExtension);
						}
						values = new String[valueList.size()];
						valueList.toArray(values);
					}
					int manualChoose =
						JOptionPane.showConfirmDialog(
							null,
							"Autogenerate each transformation");

					for (int i = 0;i < values.length;i++) { // loop through each partition type 	 
						partitionExtension = values[i];
						userTableName = (datasetName+"__"+partitionExtension.split("=")[1]+"__"+"dm").toLowerCase();

						if (chosenTable.equals(centralTableName)) {
							centralExtensionColumn = partitionExtension.split("=")[0];
							centralExtensionCondition = "=" + partitionExtension.split("=")[1];
							partitionExtension = "";
						}

						String tableType = "d";
						Integer tCount = new Integer(transformationCount + 1);

						Transformation transformation =
							new Transformation(
								tCount.toString(),
								tableType,
								centralTableName,
								userTableName,
								includeCentralFilters);

						referencedTables =
							resolver.getReferencedTables(centralTableName);
						if (i == 0)
							transformation =
								generateTransformation(
									referencedTables,
									centralTableName,
									tableType,
									datasetName,
									chosenTable,
									1,
									leftJoin,
									transformation);
						else
							transformation =
								generateTransformation(
									referencedTables,
									centralTableName,
									tableType,
									datasetName,
									chosenTable,
									manualChoose,
									leftJoin,
									transformation);

						dataset.insertChildObject(
							transformationCount,
							transformation);
						transformationCount++;

					} // end of loop
					tableList.remove(centralTableName);
					potentialTables = new String[tableList.size()];
					tableList.keySet().toArray(potentialTables);
					continue; // next dimension table candidate	
				}
			} // end of while loop over all dm candidates
			// add dataset to transformationConfig
			tConfig.insertChildObject(0, dataset);
			datasetContinueOption =
				JOptionPane.showConfirmDialog(
					null,
					"Create another dataset",
					"",
					JOptionPane.YES_NO_OPTION);
		}
		return tConfig;
	}

	private void chooseColumns(String centralTableName) {
		Box columnsBox = new Box(BoxLayout.Y_AXIS);
		Table centralTable = resolver.getCentralTable(centralTableName);
		Column[] cols = centralTable.getColumns();
		JCheckBox[] colChecks = new JCheckBox[cols.length];
		String[] colNames = new String[cols.length];
		JTextField[] colAliases = new JTextField[cols.length];
		for (int j = 0; j < cols.length; j++) {
			Box horizBox = new Box(BoxLayout.X_AXIS);
			JCheckBox check1 = new JCheckBox(cols[j].getName());
			check1.setSelected(true);
			horizBox.add(check1);
			colChecks[j] = check1;
			JTextField field1 = new JTextField(cols[j].getName());
			horizBox.add(field1);
			colNames[j] = cols[j].getName();
			colAliases[j] = field1;
			columnsBox.add(horizBox);
		}
		int colsOption =
			JOptionPane.showOptionDialog(
				null,
				columnsBox,
				"Select columns for the final dataset ",
				JOptionPane.DEFAULT_OPTION,
				JOptionPane.PLAIN_MESSAGE,
				null,
				standardOptions,
				null);
		String comma = "";
		if (colsOption == 0) {
			// recover the aliases and names into cenColAliases and celColNames
			for (int i = 0; i < colChecks.length; i++) {
				if (colChecks[i].getSelectedObjects() == null)
					continue;

				cenColName = cenColName + comma + colNames[i];
				cenColAlias = cenColAlias + comma + colAliases[i].getText();
				comma = ",";
			}
		}
	}

	private void chooseCentralTableSettings(
		String centralTableName,
		String datasetName,
		String tableType) {

		Box extensionSettings = new Box(BoxLayout.Y_AXIS);
		extensionSettings.add(Box.createRigidArea(new Dimension(700, 1)));
		Box box1 = new Box(BoxLayout.X_AXIS);
		Box box2 = new Box(BoxLayout.X_AXIS);
		JLabel label1 = new JLabel("User table name");
		box1.add(label1);
		String longTableType = "dm";
		if (tableType.equals("m"))
			longTableType = "main";
		userTableName =
			(datasetName + "__" + centralTableName + "__" + longTableType)
				.toLowerCase();
		JTextField userTableNameField = new JTextField(userTableName);
		box1.add(userTableNameField);
		extensionSettings.add(box1);
		JLabel label2 = new JLabel("Central projection/restriction (optional)");
		box2.add(label2);
		Column[] centralTableCols =
			resolver.getCentralTable(centralTableName).getColumns();
		String[] colNames = new String[centralTableCols.length];
		for (int i = 0; i < centralTableCols.length; i++) {
			colNames[i] = centralTableCols[i].getName();
		}
		JComboBox columnOptions = new JComboBox(colNames);
		box2.add(columnOptions);
		JTextField extensionField = new JTextField();
		box2.add(extensionField);
		extensionSettings.add(box2);
		int extensionOption =
			JOptionPane.showOptionDialog(
				null,
				extensionSettings,
				"Main Table Settings",
				JOptionPane.DEFAULT_OPTION,
				JOptionPane.PLAIN_MESSAGE,
				null,
				standardOptions,
				null);

		userTableName = userTableNameField.getText();
		if (!extensionField.getText().equals("")) {
			//centralExtension =
				//((String) columnOptions.getSelectedItem())
					//+ extensionField.getText();
			centralExtensionColumn = (String) columnOptions.getSelectedItem();
			centralExtensionCondition = extensionField.getText();
		}
	}

	private Transformation generateTransformation(
		Table[] referencedTables,
		String centralTableName,
		String tableType,
		String datasetName,
		String chosenTable,
		int manualChoose,
		int leftJoin,
		Transformation transformation) {

		int unitCount = 0;
		JCheckBox[] checkboxs = new JCheckBox[referencedTables.length];
		JComboBox[] comboBoxs = new JComboBox[referencedTables.length];
		JComboBox[] columnOptions = new JComboBox[referencedTables.length];
		JTextField[] textFields = new JTextField[referencedTables.length];
		JComboBox[] cenColumnOptions = new JComboBox[referencedTables.length];
		JTextField[] cenTextFields = new JTextField[referencedTables.length];
		String refTableType = "reference";

		if (manualChoose != 0) {

			if (tableType.equals("m"))
				tableList = new HashMap();
			//create a new list of candidates for next central table selection

			Box cardinalitySettings = new Box(BoxLayout.Y_AXIS);

			String[] cardinalityOptions =
				new String[] { "11", "n1", "1n", "0n", "n1r" };

			boolean seenTable = false;
			
			// GENERATE THE GUI BOX
			for (int i = 0; i < referencedTables.length; i++) {
				// we need to see central table for recursive transformations
				if (referencedTables[i].getName().equals(centralTableName)
					&& seenTable)
					continue;
				if (referencedTables[i].getName().equals(centralTableName))
					seenTable = true;
				
				Box box1 = new Box(BoxLayout.X_AXIS);
				Box box2 = new Box(BoxLayout.X_AXIS);
				Box box3 = new Box(BoxLayout.X_AXIS);
				Box box4 = new Box(BoxLayout.X_AXIS);
				checkboxs[i] =
					new JCheckBox(
						"Include "
							+ referencedTables[i].getName().toUpperCase());
				checkboxs[i].setSelected(false);
				JLabel label1 =
					new JLabel(
						"Cardinality for "
							+ centralTableName
							+ "."
							+ referencedTables[i].PK
							+ " => "
							+ referencedTables[i].getName()
							+ "."
							+ referencedTables[i].FK
							+ " ("
							+ referencedTables[i].status
							+ ")");

				comboBoxs[i] = new JComboBox(cardinalityOptions);

				HashMap cards =
					(HashMap) cardinalityFirst.get(
						referencedTables[i].getName());
				if (cards != null) {
					String cardSetting = (String) cards.get(centralTableName);
					if (cardSetting.equals("1n"))
						cardSetting = "n1";
					else if (cardSetting.equals("n1"))
						cardSetting = "1n";

					comboBoxs[i].setSelectedItem(cardSetting);
				} else {
					cards = (HashMap) cardinalityFirst.get(centralTableName);
					if (cards != null) {
						comboBoxs[i].setSelectedItem(
							cards.get(referencedTables[i].getName()));
					}
				}
				JLabel label2 =
					new JLabel("Referenced projection/restriction (optional)");
				String[] columnNames = { "%" };
				Column[] refTableCols =
					resolver.getReferencedColumns(
						referencedTables[i].getName(),
						columnNames);
				String[] colNames = new String[refTableCols.length];
				for (int j = 0; j < refTableCols.length; j++) {
					colNames[j] = refTableCols[j].getName();
				}
				columnOptions[i] = new JComboBox(colNames);
				textFields[i] = new JTextField();

				JLabel label3 =
					new JLabel("Central projection/restriction (optional)");
				Column[] centralTableCols =
					resolver.getCentralTable(centralTableName).getColumns();
				String[] cenColNames = new String[centralTableCols.length];
				for (int j = 0; j < centralTableCols.length; j++) {
					cenColNames[j] = centralTableCols[j].getName();
				}
				cenColumnOptions[i] = new JComboBox(cenColNames);
				cenTextFields[i] = new JTextField();
				if (!centralExtensionColumn.equals(""))
					cenColumnOptions[i].setSelectedItem(centralExtensionColumn);
				if (!centralExtensionCondition.equals(""))
					cenTextFields[i].setText(centralExtensionCondition);
				box1.add(checkboxs[i]);
				box1.add(new JLabel(""));
				cardinalitySettings.add(box1);
				box2.add(label1);
				box2.add(comboBoxs[i]);
				box2.setMaximumSize(new Dimension(700, 30));
				cardinalitySettings.add(box2);
				box3.add(label2);
				box3.add(columnOptions[i]);
				box3.add(textFields[i]);
				box3.setMaximumSize(new Dimension(700, 30));
				if (partitionExtension.equals(""))
					cardinalitySettings.add(box3);
				box4.add(label3);
				box4.add(cenColumnOptions[i]);
				box4.add(cenTextFields[i]);
				box4.setMaximumSize(new Dimension(700, 30));
				cardinalitySettings.add(box4);
				cardinalitySettings.add(Box.createVerticalStrut(20));
			}

			JCheckBox depthSetting =
				new JCheckBox("Go one level deeper for this central table");
			if (!tableType.equals("m")
				&& tableList.get(centralTableName) != null
				&& !tableList.get(centralTableName).equals("deepReference")) {
				cardinalitySettings.add(depthSetting);
			}

			JScrollPane scrollPane = new JScrollPane(cardinalitySettings);
			Dimension minimumSize = new Dimension(700, 500);
			scrollPane.setPreferredSize(minimumSize);

			String[] dialogOptions =
				new String[] { "Continue", "Select columns", "Cancel" };

			int option =
				JOptionPane.showOptionDialog(
					null,
					scrollPane,
					"Cardinality settings for tables referenced from "
						+ centralTableName
						+ "("
						+ partitionExtension
						+ ")",
					JOptionPane.DEFAULT_OPTION,
					JOptionPane.PLAIN_MESSAGE,
					null,
					dialogOptions,
					null);
			if (option == 2)
				return transformation;
			else if (option == 1) {
				// REFERENCE TABLE - CHOOSE COLS
				Box columnsBox = new Box(BoxLayout.Y_AXIS);
				ArrayList colChecks = new ArrayList();
				ArrayList colAliases = new ArrayList();
				ArrayList colNames = new ArrayList();
				ArrayList colTable = new ArrayList();
				for (int i = 0; i < referencedTables.length; i++) {
					Table refTab = referencedTables[i];
					if (refTab.getName().equals(centralTableName))
						continue;
					String cardinality =
						comboBoxs[i].getSelectedItem().toString();
					if (checkboxs[i].getSelectedObjects() == null
						|| cardinality.equals("1n"))
						continue;
					JLabel label1 = new JLabel(refTab.getName());
					columnsBox.add(label1);
					Column[] cols = refTab.getColumns();
					for (int j = 0; j < cols.length; j++) {
						Box horizBox = new Box(BoxLayout.X_AXIS);
						JCheckBox check1 = new JCheckBox(cols[j].getName());
						check1.setSelected(true);
						horizBox.add(check1);
						colChecks.add(check1);
						JTextField field1 = new JTextField(cols[j].getName());
						horizBox.add(field1);
						colNames.add(cols[j].getName());
						colAliases.add(field1);
						colTable.add(refTab.getName());
						columnsBox.add(horizBox);
					}
				}
				dialogOptions = new String[] { "Ok", "Cancel" };
				int colsOption =
					JOptionPane.showOptionDialog(
						null,
						columnsBox,
						"Select columns for the final dataset ",
						JOptionPane.DEFAULT_OPTION,
						JOptionPane.PLAIN_MESSAGE,
						null,
						dialogOptions,
						null);
				if (colsOption == 0) { // recover the aliases and names
					for (int i = 0; i < colChecks.size(); i++) {
						if (((JCheckBox) colChecks.get(i)).getSelectedObjects()
							== null)
							continue;

						if (refColNames.get(colTable.get(i)) == null)
							refColNames.put(colTable.get(i), colNames.get(i));
						else
							refColNames.put(
								colTable.get(i),
								refColNames.get(colTable.get(i))
									+ ","
									+ colNames.get(i));

						if (refColAliases.get(colTable.get(i)) == null)
							refColAliases.put(
								colTable.get(i),
								((JTextField) colAliases.get(i)).getText());
						else
							refColAliases.put(
								colTable.get(i),
								refColAliases.get(colTable.get(i))
									+ ","
									+ ((JTextField) colAliases.get(i)).getText());
					}
				}
			}

			if (depthSetting.getSelectedObjects() != null) {
				refTableType = "deepReference";
			}
		} // end of GUI BOX generation

		// loop through all the referenced tables to create the Transformation for the central table
		boolean seenCentralTable = false;
		for (int i = 0; i < referencedTables.length; i++) {
			Table refTab = referencedTables[i];
			//	we need to keep central table for recursive transformations
			if (refTab.getName().equals(centralTableName) && seenCentralTable)
				continue;
			if (refTab.getName().equals(centralTableName))
				seenCentralTable = true;
			
			if (checkboxs[i].getSelectedObjects() == null)
				continue;
			
			String cardinality;
			if (manualChoose != 0) {
				cardinality = comboBoxs[i].getSelectedItem().toString();
			} else {
				HashMap cards = (HashMap) cardinalityFirst.get(centralTableName);
				cardinality = (String) cards.get(referencedTables[i].getName());
			}
			
			String referencedExtension = "";
			if (refTab.getName().equals(chosenTable))
				referencedExtension = partitionExtension;
			else if (manualChoose != 0 && !textFields[i].getText().equals(""))
				referencedExtension =
					((String) columnOptions[i].getSelectedItem())
						+ textFields[i].getText();

			String centralExtension = "";
			if (cenTextFields[i] != null
				&& !cenTextFields[i].getText().equals(""))
				centralExtension =
					((String) cenColumnOptions[i].getSelectedItem())
						+ cenTextFields[i].getText();
			
			cardinalitySecond.put(refTab.getName(), cardinality);
			cardinalityFirst.put(centralTableName, cardinalitySecond);

			//if (manualChoose != 0
			//	&& ((checkboxs[i].getSelectedObjects() == null
			//		&& !refTab.getName().equals(centralTableName))
			//		|| cardinality.equals("1n"))) {
			if (manualChoose != 0 && cardinality.equals("1n")){
				if (tableType.equals("m")
					|| refTableType.equals("deepReference")) {
					tableList.put(refTab.getName(), refTableType);
				}
				continue;
			}

			if (!tableType.equals("m"))
				tableList.remove(centralTableName);

			Integer tunitCount = new Integer(unitCount + 1);
			
			String refColName = "";
			String refColAlias = "";
			if (refColNames.get(refTab.getName()) != null)
				refColName = (String) refColNames.get(refTab.getName());
			if (refColAliases.get(refTab.getName()) != null)
				refColAlias = (String) refColAliases.get(refTab.getName());

			// if potentially partitioning the central table then include as a candidate in next list
			if ((!centralExtension.equals("") || !referencedExtension.equals(""))
				&& refTab.getName().equals(centralTableName))
				tableList.put(centralTableName, "m");

			TransformationUnit transformationUnit =
							new TransformationUnit(
								tunitCount.toString(),
								refTab.status,
								refTab.PK,
								refTab.getName(),
								cardinality,
								centralExtension,
								referencedExtension,
								refTab.FK,
								refColName,
								refColAlias,
								cenColName,
								cenColAlias);

			transformation.insertChildObject(unitCount, transformationUnit);
			unitCount++;

		}

		if (leftJoin == 1) {
			Integer tunitCount = new Integer(unitCount + 1);
			TransformationUnit extraUnit =
				new TransformationUnit(
					tunitCount.toString(),
					"exported",
					resolver.getCentralTable(centralTableName).PK,
					"main_interim",
					"n1r",
					"",
					"",
					resolver.getCentralTable(centralTableName).PK,
					resolver.getCentralTable(centralTableName).PK,
					"",
					"",
					"");

			transformation.insertChildObject(unitCount, extraUnit);
		}

		if (transformation.getChildObjects().length == 0) {
			// no ref tables for this transformation
			TransformationUnit transformationUnit =
							new TransformationUnit(
								"1",
								"",
								"",
								"",
								"",
								centralExtensionColumn+centralExtensionCondition,
								"",
								"",
								"",
								"",
								cenColName,
								cenColAlias);
			transformation.insertChildObject(unitCount, transformationUnit);
		}
		return transformation;
	}
			
}
