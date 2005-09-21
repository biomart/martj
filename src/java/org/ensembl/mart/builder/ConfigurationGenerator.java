/*
 * Created on Sep 1, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.ensembl.mart.builder;

import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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
import org.ensembl.mart.builder.lib.ConfigurationBase;
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
public class ConfigurationGenerator implements ItemListener{

	private HashMap tableList = new HashMap();
	private HashMap refColNames = new HashMap();
	private HashMap refColAliases = new HashMap();
	private HashMap cardinalityFirst = new HashMap();
	private HashMap cardinalitySecond = new HashMap();
	private String cenColName = "";
	private String cenColAlias = "";
	private String userTableName;
	private String centralExtensionColumn;
	private String centralExtensionOperator;
	private String centralExtensionValue;
	private String partitionExtension;
	private int leftJoin = 0;
	private String[] dialogOptions = new String[] { "Continue", "Select columns", "Finish"};
	private String[] extendedDialogOptions = new String[] { "Continue", "Select columns", "Finish","Skip"};
	private String[] standardOptions = new String[] { "OK", "Cancel" };
	private String[] includeCentralFilterOptions = new String[] { "N", "Y" };
	private String[] opOptions = new String [] {"=",">","<",">=","<=","like"};
	private String[] cardinalityOptions = new String[] { "1n", "11", "n1", "0n", "n1r" };
	private MetaDataResolver resolver;
	private String schema, externalSchema;
	private Table[] referencedTables;
	private DatabaseAdaptor adaptor;
	
	private JComboBox tableOptions, pK, fK, extSchema, tableNameBox, dmTableNameBox, partitionColsOption, status,
		userCardinality, userRefCol, userRefOperator, userCenCol, userCenOperator;
	private JCheckBox includeUserDefined, userKeep, goDeeper;
	private JTextField userRefText, userCenText;
	
	private JCheckBox[] checkboxs;
	private JComboBox[] comboBoxs;
	private JComboBox[] columnOptions;
	private JComboBox[] operatorOptions;
	private JTextField[] textFields;
	private JComboBox[] cenColumnOptions;
	private JComboBox[] cenOperatorOptions;
	private JTextField[] cenTextFields;
	private JCheckBox[] keepCheckBoxs;
	private JCheckBox[] goDeeperCheckBoxs;
	
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
		String[] datasetPartitionValues = null;
		// cycle through adding datasets to this transformation config
		int datasetContinueOption = 0;
		while (datasetContinueOption != 1) {
			int transformationCount = 0;
			// MAIN TABLE - DATASET, MAIN TABLE NAME AND PARTITIONING SETTINGS 
			String[] potentialTables = resolver.getAllTableNames();
			Box centralSettings = new Box(BoxLayout.Y_AXIS);
			centralSettings.add(Box.createRigidArea(new Dimension(400, 1)));
			Box box2 = new Box(BoxLayout.X_AXIS);
			JLabel label2 = new JLabel("Dataset name");
			JTextField datasetField = new JTextField("test");
			box2.add(label2);
			box2.add(datasetField);
			Box box1 = new Box(BoxLayout.X_AXIS);
			JLabel label1 = new JLabel("Name  ");
			box1.add(label1);
			tableNameBox = new JComboBox(potentialTables);
			tableNameBox.addItemListener(this);
			box1.add(tableNameBox);
			centralSettings.add(box2);
			centralSettings.add(box1);
			Box box4 = new Box(BoxLayout.X_AXIS);
			JCheckBox partitionBox = new JCheckBox("Dataset partitioning on");
			box4.add(partitionBox);				
			ArrayList allCols = new ArrayList();
			referencedTables =
				resolver.getReferencedTables((String) tableNameBox.getSelectedItem());
			int centralSeen = 0;
			for (int k = 0; k < referencedTables.length; k++) {
				if (centralSeen > 0 && referencedTables[k].getName().equals((String)tableNameBox.getSelectedItem()))
					continue;
				Column[] tableCols = referencedTables[k].getColumns();
				for (int l = 0; l < tableCols.length; l++) {
					String entry = referencedTables[k].getName()+"."+ tableCols[l].getName();
					allCols.add(entry);
				}
				if (referencedTables[k].getName().equals((String) tableNameBox.getSelectedItem()))
					centralSeen++;
			}
			String[] cols = new String[allCols.size()];	
			allCols.toArray(cols);		
			partitionColsOption = new JComboBox(cols);
			box4.add(partitionColsOption);
			centralSettings.add(box4);
			
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
					
			String datasetName = datasetField.getText();							
			if (datasetName == null)
				return tConfig;
			Dataset dataset = new Dataset(datasetName, "");

			String centralTableName = tableNameBox.getSelectedItem().toString();
			if (option2 == 2)
				break;
			else if (option2 == 1)
				chooseColumns(centralTableName);

			partitionExtension = "";
			centralExtensionColumn = "";
			//centralExtensionCondition = "";
			centralExtensionOperator = "";
			centralExtensionValue = "";
			
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
				referencedTables =
					resolver.getReferencedTables(centralTableName);
				transformation =
					generateTransformation(
						//referencedTables,
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
					
				String[] chosenOptions = ((String) partitionColsOption.getSelectedItem()).split("\\.+");
				String chosenTable = chosenOptions[0];
				String chosenColumn = chosenOptions[1];

				ArrayList allValList = 	resolver.getDistinctValuesForPartitioning(
											chosenColumn,
											chosenTable);
					
				int manualChoose;
				if (allValList.size() > 20 || allValList.size() == 0) {// hack for empty tables during dev			
					Box colOps = new Box(BoxLayout.Y_AXIS);
					label1 = new JLabel("Too many values to display - enter comma separated list");
					colOps.add(label1);
					JTextField userValues = new JTextField();
					colOps.add(userValues);
					manualChoose =
						JOptionPane.showOptionDialog(
												null,
												colOps,
												"Select values for partitioning ",
												JOptionPane.DEFAULT_OPTION,
												JOptionPane.PLAIN_MESSAGE,
												null,
												standardOptions,
												null);
					String[] indValues = userValues.getText().split(",");
					datasetPartitionValues = new String[indValues.length];
					for (int i = 0; i < indValues.length; i++) {
						datasetPartitionValues[i] = chosenColumn + "=" + indValues[i];
					}
				} 
				else {
					Box colOps = new Box(BoxLayout.Y_AXIS);
					JCheckBox[] checks = new JCheckBox[allValList.size()];
					for (int i = 0; i < allValList.size(); i++) {
						checks[i] = new JCheckBox((String) allValList.get(i));
						checks[i].setSelected(true);
						colOps.add(checks[i]);
					}
					manualChoose =
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
					datasetPartitionValues = new String[valueList.size()];
					valueList.toArray(datasetPartitionValues);
				}
				
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
				referencedTables = resolver.getReferencedTables(centralTableName);
				transformation = generateTransformation(
										//referencedTables,
										centralTableName,
										tableType,
										datasetName,
										"",
										1,
										leftJoin,
										transformation);
				dataset.insertChildObject(transformationCount, transformation);
				transformationCount++;
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
				//centralExtensionCondition = "";
				centralExtensionOperator = "";
				centralExtensionValue = "";
				cenColName = "";
				cenColAlias = "";

				centralSettings = new Box(BoxLayout.Y_AXIS);
				centralSettings.add(Box.createRigidArea(new Dimension(400, 1)));
				box1 = new Box(BoxLayout.X_AXIS);
				box2 = new Box(BoxLayout.X_AXIS);
				Box box3 = new Box(BoxLayout.X_AXIS);
				label1 = new JLabel("Name  ");
				box1.add(label1);
				dmTableNameBox = new JComboBox(potentialTables);
				dmTableNameBox.addItemListener(this);
				box1.add(dmTableNameBox);
				centralSettings.add(box1);
				JLabel label3 = new JLabel("Include central filters?");
				box3.add(label3);
				JComboBox includeCentralBox =
					new JComboBox(includeCentralFilterOptions);
				box3.add(includeCentralBox);
				centralSettings.add(box3);
				Box box5 = new Box(BoxLayout.X_AXIS);
				JLabel label4 = new JLabel("Use left join?");
				box5.add(label4);
				JComboBox leftJoinBox = new JComboBox(includeCentralFilterOptions);
				box5.add(leftJoinBox);
				centralSettings.add(box5);
				box4 = new Box(BoxLayout.X_AXIS);
				partitionBox = new JCheckBox("Partitioning on");
				box4.add(partitionBox);				
				allCols = new ArrayList();
				referencedTables =
					resolver.getReferencedTables((String) dmTableNameBox.getSelectedItem());
				centralSeen = 0;
				for (int k = 0; k < referencedTables.length; k++) {
					if (centralSeen > 0 && referencedTables[k].getName().equals((String) 
							dmTableNameBox.getSelectedItem()))
						continue;
					Column[] tableCols = referencedTables[k].getColumns();
					for (int l = 0; l < tableCols.length; l++) {
						String entry = referencedTables[k].getName()+"."+ tableCols[l].getName();
						allCols.add(entry);
					}
					if (referencedTables[k].getName().equals((String) dmTableNameBox.getSelectedItem()))
						centralSeen++;
				}
				cols = new String[allCols.size()];	
				allCols.toArray(cols);		
				partitionColsOption = new JComboBox(cols);
				box4.add(partitionColsOption);
				centralSettings.add(box4);
				
				int option3 =
					JOptionPane.showOptionDialog(
						null,
						centralSettings,
						"Dimension Table Settings",
						JOptionPane.DEFAULT_OPTION,
						JOptionPane.PLAIN_MESSAGE,
						null,
						extendedDialogOptions,
						null);

				centralTableName = dmTableNameBox.getSelectedItem().toString();
				String includeCentralFilters =
					includeCentralBox.getSelectedItem().toString();

				if (option3 == 3 ){
					
					// you can skip candidates here
					tableList.remove(centralTableName);
					potentialTables = new String[tableList.size()];
					tableList.keySet().toArray(potentialTables);
					continue; // next dimension table candidate	
					
				}
				if (option3 == 2)
					break;
				if (option3 == 1)
					chooseColumns(centralTableName);
				

				if (leftJoinBox.getSelectedItem().toString().equals("Y"))
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

					referencedTables =
						resolver.getReferencedTables(centralTableName);
					transformation =
						generateTransformation(
							//referencedTables,
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

					potentialTables = new String[tableList.size()];
					tableList.keySet().toArray(potentialTables);
				} else {
					// DIMENSION TABLE - DO PARTITIONING
					String[] chosenOptions = ((String) partitionColsOption.getSelectedItem()).split("\\.+");
					String chosenTable = chosenOptions[0];
					String chosenColumn = chosenOptions[1];

					ArrayList allValList =
						resolver.getDistinctValuesForPartitioning(
							chosenColumn,
							chosenTable);

					String[] values;
					String[] manualOptions = new String[] {"Autogenerate","Manually choose"};							
					int manualChoose;			
								
					if (allValList.size() > 20 || allValList.size() == 0) {// hack for empty tables during dev			
						Box colOps = new Box(BoxLayout.Y_AXIS);
					    label1 = new JLabel("Too many values to display - enter comma separated list");
						colOps.add(label1);
						JTextField userValues = new JTextField();
						colOps.add(userValues);
						manualChoose =
							JOptionPane.showOptionDialog(
								null,
								colOps,
								"Select values for partitioning ",
								JOptionPane.DEFAULT_OPTION,
								JOptionPane.PLAIN_MESSAGE,
								null,
								manualOptions,
								null);
						String[] indValues = userValues.getText().split(",");
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
						manualChoose =
							JOptionPane.showOptionDialog(
								null,
								colOps,
								"Select values for partitioning ",
								JOptionPane.DEFAULT_OPTION,
								JOptionPane.PLAIN_MESSAGE,
								null,
								manualOptions,
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
					
					for (int i = 0;i < values.length;i++) { // loop through each partition type 	 
						partitionExtension = values[i];
						userTableName=(datasetName+"__"+partitionExtension.split("=")[1]+"__"+"dm").toLowerCase();

						if (chosenTable.equals(centralTableName)) {
							centralExtensionColumn = partitionExtension.split("=")[0];
							centralExtensionValue = partitionExtension.split("=")[1];
							centralExtensionOperator = "=";
							partitionExtension = "";
						}
						else{
							partitionExtension = partitionExtension.split("=")[0]
								+ "='" + partitionExtension.split("=")[1] + "'";
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
									//referencedTables,
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
									//referencedTables,
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
					
					potentialTables = new String[tableList.size()];
					tableList.keySet().toArray(potentialTables);
					continue; // next dimension table candidate	
				}
			} // end of while loop over all dm candidates
			// add dataset to transformationConfig
			if (datasetPartitionValues != null){
				Dataset[] partitionedDatasets = new Dataset[datasetPartitionValues.length];
				Transformation[] mainTransformation = new Transformation[datasetPartitionValues.length];
				for (int i = 0; i < datasetPartitionValues.length; i++){
					// note copy method didn't work for dataset and transformation
					partitionedDatasets[i] = new Dataset(dataset.getElement().getAttributeValue("internalName")+"_"+
						datasetPartitionValues[i].split("=")[1],dataset.getElement().getAttributeValue("mainTable"));	
					ConfigurationBase[] transformations = dataset.getChildObjects();
					mainTransformation[i] = new Transformation(
						transformations[0].getElement().getAttributeValue("internalName"),
						transformations[0].getElement().getAttributeValue("tableType"),
						transformations[0].getElement().getAttributeValue("centralTable"),
						transformations[0].getElement().getAttributeValue("userTableName"),
						transformations[0].getElement().getAttributeValue("includeCentralFilter"));
					ConfigurationBase[] transformationUnits = transformations[0].getChildObjects();
					for (int k = 0; k < transformationUnits.length; k++){
						TransformationUnit tUnit = (TransformationUnit) transformationUnits[k].copy();
						tUnit.getElement().setAttribute("centralProjection",datasetPartitionValues[i]);
						mainTransformation[i].addChildObject(tUnit);	
					}
					partitionedDatasets[i].addChildObject(mainTransformation[i]);
					for (int k = 1;k < transformations.length;k++){
						partitionedDatasets[i].addChildObject(transformations[k]);
					}
					tConfig.insertChildObject(i, partitionedDatasets[i]);
				}
			}
			else{
				tConfig.insertChildObject(0, dataset);
			}
			
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
		
		JComboBox operatorOptions = new JComboBox(opOptions);
		box2.add(operatorOptions);
		
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
			centralExtensionColumn = (String) columnOptions.getSelectedItem();
			centralExtensionOperator = (String) operatorOptions.getSelectedItem();
			centralExtensionValue = extensionField.getText();
		}
	}

	private Transformation generateTransformation(
		String centralTableName,
		String tableType,
		String datasetName,
		String chosenTable,
		int manualChoose,
		int leftJoin,
		Transformation transformation) {

		String externalSchema = "";
		int unitCount = 0;
		
		checkboxs = new JCheckBox[referencedTables.length+1];//incase user defined table added
		comboBoxs = new JComboBox[referencedTables.length+1];
		columnOptions = new JComboBox[referencedTables.length+1];
		operatorOptions = new JComboBox[referencedTables.length+1];
		textFields = new JTextField[referencedTables.length+1];
		cenColumnOptions = new JComboBox[referencedTables.length+1];
		cenOperatorOptions = new JComboBox[referencedTables.length+1];
		cenTextFields = new JTextField[referencedTables.length+1];
		keepCheckBoxs = new JCheckBox[referencedTables.length+1];
		goDeeperCheckBoxs = new JCheckBox[referencedTables.length+1];
				
		if (manualChoose != 0) {
			if (tableType.equals("m"))
				tableList = new HashMap();
			//create a new list of candidates for next central table selection
			Box cardinalitySettings = new Box(BoxLayout.Y_AXIS);
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
				Box box5 = new Box(BoxLayout.X_AXIS);
				Box box6 = new Box(BoxLayout.X_AXIS);
	
				checkboxs[i] =
					new JCheckBox(
						"Include "
							+ referencedTables[i].getName().toUpperCase());
				checkboxs[i].setSelected(true);
				JLabel label1 =	new JLabel("Cardinality for "+centralTableName+"."+referencedTables[i].FK
							+" => "+ referencedTables[i].getName()+"."+referencedTables[i].PK
							+" ("+ referencedTables[i].status+ ")");
				comboBoxs[i] = new JComboBox(cardinalityOptions);

				HashMap cards =
					(HashMap) cardinalityFirst.get(
						referencedTables[i].getName());
				if (cards != null && cards.get(centralTableName) != null) {
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
				operatorOptions[i] = new JComboBox(opOptions);
				textFields[i] = new JTextField();

				JLabel label3 = new JLabel("Central projection/restriction (optional)");
				Column[] centralTableCols = resolver.getCentralTable(centralTableName).getColumns();
				String[] cenColNames = new String[centralTableCols.length];
				for (int j = 0; j < centralTableCols.length; j++) {
					cenColNames[j] = centralTableCols[j].getName();
				}
				cenColumnOptions[i] = new JComboBox(cenColNames);
				cenOperatorOptions[i] = new JComboBox(opOptions);
				cenTextFields[i] = new JTextField();
				if (!centralExtensionColumn.equals(""))
					cenColumnOptions[i].setSelectedItem(centralExtensionColumn);
				if (!centralExtensionOperator.equals(""))
					cenOperatorOptions[i].setSelectedItem(centralExtensionOperator);	
				if (!centralExtensionValue.equals(""))
					cenTextFields[i].setText(centralExtensionValue);
					
				keepCheckBoxs[i] = new JCheckBox("Always keep for subsequent transformations");	
				goDeeperCheckBoxs[i] = new JCheckBox("Go deeper if 11 or n1");	
				
				box1.add(checkboxs[i]);
				box1.add(new JLabel(""));
				cardinalitySettings.add(box1);
				box2.add(label1);
				box2.add(comboBoxs[i]);
				box2.setMaximumSize(new Dimension(750, 30));
				cardinalitySettings.add(box2);
				box3.add(label2);
				box3.add(columnOptions[i]);
				box3.add(operatorOptions[i]);
				box3.add(textFields[i]);
				box3.setMaximumSize(new Dimension(750, 30));
				if (partitionExtension.equals(""))
					cardinalitySettings.add(box3);
				box4.add(label3);
				box4.add(cenColumnOptions[i]);
				box4.add(cenOperatorOptions[i]);
				box4.add(cenTextFields[i]);
				box4.setMaximumSize(new Dimension(750, 30));
				cardinalitySettings.add(box4);
				box5.add(keepCheckBoxs[i]);
				cardinalitySettings.add(box5);
				// want to allow central recursive joins so remove
				//Table[] downstreamTables = resolver.getReferencedTables(referencedTables[i].getName());
				//for (int k = 0; k < downstreamTables.length ;k++){
				//	if (downstreamTables[k].getName().equals(referencedTables[i].getName())
				//		|| downstreamTables[k].getName().equals(centralTableName))
				//			continue;
					 			
					box6.add(goDeeperCheckBoxs[i]);
					cardinalitySettings.add(box6);
				//	break;
				//}		

				cardinalitySettings.add(Box.createVerticalStrut(20));
			}

//			new code start
			Box box6 = new Box(BoxLayout.X_AXIS);
			Box box7 = new Box(BoxLayout.X_AXIS);
			Box box8 = new Box(BoxLayout.X_AXIS);
			Box box9 = new Box(BoxLayout.X_AXIS);
			Box box10 = new Box(BoxLayout.X_AXIS);
			Box box11 = new Box(BoxLayout.X_AXIS);
			Box box12 = new Box(BoxLayout.X_AXIS);
			Box box13 = new Box(BoxLayout.X_AXIS);
			Box box14 = new Box(BoxLayout.X_AXIS);
			Box box15 = new Box(BoxLayout.X_AXIS);
									 
			includeUserDefined = new JCheckBox("Include user defined table");
			cardinalitySettings.add(includeUserDefined);
			includeUserDefined.addItemListener(this);
			JLabel label1 = new JLabel("External schema");
			extSchema = new JComboBox(resolver.getAllSchemas());
			extSchema.setSelectedItem(adaptor.getSchema());
			extSchema.addItemListener(this);
			box6.add(label1);
			box6.add(extSchema);
			JLabel label2 = new JLabel("Table");
			String[] tableNames = resolver.getAllTableNames();
			tableOptions = new JComboBox(tableNames);			
			tableOptions.addItemListener(this);
			box7.add(label2);
			box7.add(tableOptions);
			JLabel label3 = new JLabel("Primary Key");
			String addedTableName = (String) tableOptions.getSelectedItem();
			Column[] tableCols = resolver.getCentralTable(addedTableName).getColumns();
			String[] userColNames = new String[tableCols.length];
			for (int j = 0; j < tableCols.length; j++) {
				 userColNames[j] = tableCols[j].getName();
			}
			pK = new JComboBox(userColNames);
			box8.add(label3);
			box8.add(pK);			
			JLabel label4 = new JLabel("Foreign Key");
			fK = new JComboBox(userColNames);
			box9.add(label4);
			box9.add(fK);	
			JLabel label5 = new JLabel("Status");
			String[] statusOptions = new String[] {"exported","imported"};
			status = new JComboBox(statusOptions);
			box10.add(label5);
			box10.add(status);
			
			label1 = new JLabel("Cardinality");
			userCardinality = new JComboBox(cardinalityOptions);
			box11.add(label1);
			box11.add(userCardinality);
			
			label2 = new JLabel("Referenced projection/restriction (optional)");
			userRefCol = new JComboBox(userColNames);
			userRefOperator = new JComboBox(opOptions);
			userRefText = new JTextField();
			box12.add(label2);
			box12.add(userRefCol);
			box12.add(userRefOperator);
			box12.add(userRefText);
			
			label3 = new JLabel("Central projection/restriction (optional)");
			Column[] centralTableCols =
				resolver.getCentralTable(centralTableName).getColumns();
			String[] cenColNames = new String[centralTableCols.length];
			for (int j = 0; j < centralTableCols.length; j++) {
				cenColNames[j] = centralTableCols[j].getName();
			}
			userCenCol = new JComboBox(cenColNames);
			userCenOperator = new JComboBox(opOptions);
			userCenText = new JTextField();
			if (!centralExtensionColumn.equals(""))
				userCenCol.setSelectedItem(centralExtensionColumn);
			if (!centralExtensionOperator.equals(""))
				userCenOperator.setSelectedItem(centralExtensionOperator);	
			if (!centralExtensionValue.equals(""))
				userCenText.setText(centralExtensionValue);
			box13.add(label3);
			box13.add(userCenCol);
			box13.add(userCenOperator);
			box13.add(userCenText);
					
			userKeep = new JCheckBox("Always keep for subsequent transformations");	
			box14.add(userKeep);
			
			goDeeper = new JCheckBox("Go deeper if 11 or n1");	
			box15.add(goDeeper);
	
			cardinalitySettings.add(box6);
			cardinalitySettings.add(box7);
			cardinalitySettings.add(box8);
			cardinalitySettings.add(box9);
			cardinalitySettings.add(box10);	
			cardinalitySettings.add(box11);
			cardinalitySettings.add(box12);
			cardinalitySettings.add(box13);
			cardinalitySettings.add(box14);	
			cardinalitySettings.add(box15);			 
			// end of new code

			JScrollPane scrollPane = new JScrollPane(cardinalitySettings);
			Dimension minimumSize = new Dimension(750, 500);
			scrollPane.setPreferredSize(minimumSize);

			String[] dialogOptions =
				new String[] { "Continue", "Select columns", "Cancel"};

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
					label1 = new JLabel(refTab.getName());
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
		} // end of GUI BOX generation
	
		// deal with user defined table
		if (includeUserDefined.getSelectedObjects() != null){
			 // create table and add to referencedTables	
			 Table addedTable = new Table();
			 addedTable.setName(((String) tableOptions.getSelectedItem()));
			 addedTable.PK = (String) pK.getSelectedItem();
			 addedTable.FK = (String) fK.getSelectedItem();
			 addedTable.status = (String) status.getSelectedItem();
			 Table[] newReferencedTables = new Table[referencedTables.length+1];
		 
			 for (int i = 0; i < referencedTables.length; i++){
				 newReferencedTables[i] = referencedTables[i];
			 }
			 newReferencedTables[newReferencedTables.length-1] = addedTable;
			 referencedTables = newReferencedTables;
	
			 if (!((String) extSchema.getSelectedItem()).equals(adaptor.getSchema()))
				 externalSchema = (String) extSchema.getSelectedItem();
					 
			 // need to add checkbox and cardinality settings etc for processing of tables
			 checkboxs[referencedTables.length-1] = new JCheckBox();
			 checkboxs[referencedTables.length-1].setSelected(true);
			 comboBoxs[referencedTables.length-1] = userCardinality;
			 columnOptions[referencedTables.length-1] = userRefCol;
			 operatorOptions[referencedTables.length-1] = userRefOperator;
			 cenColumnOptions[referencedTables.length-1] = userCenCol;
			 cenOperatorOptions[referencedTables.length-1] = userCenOperator;
			 textFields[referencedTables.length-1] = userRefText;
			 cenTextFields[referencedTables.length-1] = userCenText;
			 keepCheckBoxs[referencedTables.length-1] = userKeep;	 
			 goDeeperCheckBoxs[referencedTables.length-1] = goDeeper;	 
		}
			
		// loop through all the referenced tables to create the Transformation for the central table
		boolean seenCentralTable = false;
		for (int i = 0; i < referencedTables.length; i++) {
			int a = referencedTables.length - 1;
			Table refTab = referencedTables[i];
			//	we need to keep central table for recursive transformations
			if (refTab.getName().equals(centralTableName) && seenCentralTable)
				continue;
			if (refTab.getName().equals(centralTableName))
				seenCentralTable = true;
			
			if (manualChoose != 0 && checkboxs[i].getSelectedObjects() == null){
				tableList.remove(refTab.getName());// remove from the candidate list if present
				HashMap cards = (HashMap) cardinalityFirst.get(centralTableName);
				if (cards != null)
					cards.remove(referencedTables[i].getName());
				continue;
			}
			
			String cardinality;
			if (manualChoose != 0) {
				cardinality = comboBoxs[i].getSelectedItem().toString();
			} else {
				HashMap cards = (HashMap) cardinalityFirst.get(centralTableName);
				cardinality = (String) cards.get(referencedTables[i].getName());
				// if cardinality null means this table was not checked - hence skip
				if (cardinality == null)
					continue;	
			}		
			
			String referencedExtension = "";
			if (refTab.getName().equals(chosenTable))
				referencedExtension = partitionExtension;
			else if (manualChoose != 0 && !textFields[i].getText().equals(""))
				referencedExtension =
					((String) columnOptions[i].getSelectedItem())
					+ ((String) operatorOptions[i].getSelectedItem())
					+ "'" + textFields[i].getText() + "'";

			String centralExtension = "";
			if (cenTextFields[i] != null
				&& !cenTextFields[i].getText().equals(""))
				centralExtension =
					((String) cenColumnOptions[i].getSelectedItem())
					 + ((String) cenOperatorOptions[i].getSelectedItem())
					 + "'"+cenTextFields[i].getText()+"'";
			
			cardinalitySecond.put(refTab.getName(), cardinality);
			cardinalityFirst.put(centralTableName, cardinalitySecond);

			if (manualChoose != 0 && keepCheckBoxs[i].getSelectedObjects() != null)
				tableList.put(refTab.getName(), "reference");

			if (cardinality.equals("1n")){	
				if (tableType.equals("m")) {
					tableList.put(refTab.getName(), "reference");
				}
				continue;
			}
						
			if (manualChoose != 0 && refTab.getName().equals(centralTableName) && 
					keepCheckBoxs[i].getSelectedObjects() == null)
				tableList.remove(centralTableName);
			
			Integer tunitCount = new Integer(unitCount + 1);
			
			String refColName = "";
			String refColAlias = "";
			if (refColNames.get(refTab.getName()) != null)
				refColName = (String) refColNames.get(refTab.getName());
			if (refColAliases.get(refTab.getName()) != null)
				refColAlias = (String) refColAliases.get(refTab.getName());

			

			if (i != 0){// externalSchema only applies for user defined tables which are always first in the array
				externalSchema= "";	
			}
			
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
								cenColAlias,
								externalSchema);

			transformation.insertChildObject(unitCount, transformationUnit);
			unitCount++;
			
			if (goDeeperCheckBoxs[i].getSelectedObjects() != null){
				TransformationUnit[] deeperUnits = getDeeperUnits(refTab.getName(), centralTableName, unitCount);
				for (int k = 0; k < deeperUnits.length; k++){
					transformation.insertChildObject(unitCount,deeperUnits[k]);
					unitCount++;
				}
			}
			
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
								centralExtensionColumn+centralExtensionOperator+"'"+centralExtensionValue+"'",
								"",
								"",
								"",
								"",
								cenColName,
								cenColAlias,
								"");
			transformation.insertChildObject(unitCount, transformationUnit);
		}
		return transformation;
	}
	
	private TransformationUnit[] getDeeperUnits(String refTableName, String centralTableName, int unitCount){
	
		// LAUNCH THE GUI WINDOW
		Table[] potentialDeeperTables = resolver.getReferencedTables(refTableName);
		
		JCheckBox[] includeCheckBoxs = new JCheckBox[potentialDeeperTables.length];
		JComboBox[] cardinalityComboBoxs = new JComboBox[potentialDeeperTables.length];
		String[] deepCardinalityOptions = new String[] { "11", "n1" };
		JComboBox[] deepColumnOptions = new JComboBox[potentialDeeperTables.length];
		JComboBox[] deepOperatorOptions = new JComboBox[potentialDeeperTables.length];
		JTextField[] deepTextFields = new JTextField[potentialDeeperTables.length];
		JComboBox[] deepCenColumnOptions = new JComboBox[potentialDeeperTables.length];
		JComboBox[] deepCenOperatorOptions = new JComboBox[potentialDeeperTables.length];
		JTextField[] deepCenTextFields = new JTextField[potentialDeeperTables.length];
		JCheckBox[] deepGoDeeperCheckBoxs = new JCheckBox[potentialDeeperTables.length];
		
		Box cardinalitySettings = new Box(BoxLayout.Y_AXIS);
		boolean seenTable = false;					
		for (int i = 0; i < potentialDeeperTables.length; i++) {
			if (potentialDeeperTables[i].getName().equals(refTableName) || 
				(potentialDeeperTables[i].getName().equals(centralTableName) && seenTable))
					 continue;
			
			if (potentialDeeperTables[i].getName().equals(centralTableName))
					seenTable = true;
			
			
				
			Box box1 = new Box(BoxLayout.X_AXIS);
			Box box2 = new Box(BoxLayout.X_AXIS);
			Box box3 = new Box(BoxLayout.X_AXIS);
			Box box4 = new Box(BoxLayout.X_AXIS);
			Box box5 = new Box(BoxLayout.X_AXIS);
			Box box6 = new Box(BoxLayout.X_AXIS);
	
			includeCheckBoxs[i] = new JCheckBox("Include "+ potentialDeeperTables[i].getName().toUpperCase());
			JLabel label1 =	new JLabel("Cardinality for "+refTableName+"."+potentialDeeperTables[i].FK
					 +" => "+ potentialDeeperTables[i].getName()+"."+potentialDeeperTables[i].PK
					 +" ("+ potentialDeeperTables[i].status+ ")");
			cardinalityComboBoxs[i] = new JComboBox(deepCardinalityOptions);
					 					 
			JLabel label2 =	new JLabel("Referenced projection/restriction (optional)");
			String[] columnNames = { "%" };
			Column[] refTableCols = resolver.getReferencedColumns(potentialDeeperTables[i].getName(),columnNames);
			String[] colNames = new String[refTableCols.length];
			for (int j = 0; j < refTableCols.length; j++) {
				 colNames[j] = refTableCols[j].getName();
			}
					 
			deepColumnOptions[i] = new JComboBox(colNames);
			deepOperatorOptions[i] = new JComboBox(opOptions);
			deepTextFields[i] = new JTextField();
					 
			// ? IF SHOULD BE CENTRAL OR REF TABLE
			JLabel label3 = new JLabel("Central projection/restriction (optional)");
			Column[] centralTableCols = resolver.getCentralTable(refTableName).getColumns();
			String[] cenColNames = new String[centralTableCols.length];
			for (int j = 0; j < centralTableCols.length; j++) {
				 cenColNames[j] = centralTableCols[j].getName();
			}
					 
			deepCenColumnOptions[i] = new JComboBox(cenColNames);
			deepCenOperatorOptions[i] = new JComboBox(opOptions);
			deepCenTextFields[i] = new JTextField();
					 
			// ? IF SHOULD USE			 
			if (!centralExtensionColumn.equals(""))
				 cenColumnOptions[i].setSelectedItem(centralExtensionColumn);
			if (!centralExtensionOperator.equals(""))
				 cenOperatorOptions[i].setSelectedItem(centralExtensionOperator);	
			if (!centralExtensionValue.equals(""))
				 cenTextFields[i].setText(centralExtensionValue);
					
			deepGoDeeperCheckBoxs[i] = new JCheckBox("Go deeper if 11 or n1");	
				
			box1.add(includeCheckBoxs[i]);
			box1.add(new JLabel(""));
			cardinalitySettings.add(box1);
			box2.add(label1);
			box2.add(cardinalityComboBoxs[i]);
			box2.setMaximumSize(new Dimension(750, 30));
			cardinalitySettings.add(box2);
			box3.add(label2);
			box3.add(columnOptions[i]);
			box3.add(operatorOptions[i]);
			box3.add(textFields[i]);
			box3.setMaximumSize(new Dimension(750, 30));
			if (partitionExtension.equals(""))
				 cardinalitySettings.add(box3);
			box4.add(label3);
			box4.add(cenColumnOptions[i]);
			box4.add(cenOperatorOptions[i]);
			box4.add(cenTextFields[i]);
			box4.setMaximumSize(new Dimension(750, 30));
			cardinalitySettings.add(box4);
			// want to allow recursive central choice so remove
			//Table[] downstreamTables = resolver.getReferencedTables(potentialDeeperTables[i].getName());
			//for (int k = 0; k < downstreamTables.length ;k++){
			 //	if (downstreamTables[k].getName().equals(potentialDeeperTables[i].getName())
			 	//	|| downstreamTables[k].getName().equals(refTableName))
			 	//		continue;
					 			
			 	box6.add(deepGoDeeperCheckBoxs[i]);
			 	cardinalitySettings.add(box6);
			 	//break;
			//}				
			cardinalitySettings.add(Box.createVerticalStrut(20));
		}
		
		JScrollPane scrollPane = new JScrollPane(cardinalitySettings);
		Dimension minimumSize = new Dimension(750, 500);
		scrollPane.setPreferredSize(minimumSize);

		String[] dialogOptions = new String[] { "Continue", "Select columns", "Cancel"};

		int option = JOptionPane.showOptionDialog(
							null,
							scrollPane,
							"Select any 11 or n1 tables you want to include downstream of "+refTableName,
							JOptionPane.DEFAULT_OPTION,
							JOptionPane.PLAIN_MESSAGE,
							null,
							dialogOptions,
							null);
		
		
		
		if (option == 2){
			//return transformation;
		}
		else if (option == 1) {
			// REFERENCE TABLE - CHOOSE COLS
			Box columnsBox = new Box(BoxLayout.Y_AXIS);
			ArrayList colChecks = new ArrayList();
			ArrayList colAliases = new ArrayList();
			ArrayList colNames = new ArrayList();
			ArrayList colTable = new ArrayList();
			seenTable = false;
			for (int i = 0; i < potentialDeeperTables.length; i++) {
				Table refTab = potentialDeeperTables[i];
				//if (refTab.getName().equals(refTableName)
				//	|| refTab.getName().equals(centralTableName))
				//		continue;
						
				if (potentialDeeperTables[i].getName().equals(refTableName) || 
						(potentialDeeperTables[i].getName().equals(centralTableName) && seenTable))
							 continue;
			
				if (potentialDeeperTables[i].getName().equals(centralTableName))
						seenTable = true;		
						
						
											
				String cardinality = cardinalityComboBoxs[i].getSelectedItem().toString();
				if (includeCheckBoxs[i].getSelectedObjects() == null || cardinality.equals("1n"))
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
				if (((JCheckBox) colChecks.get(i)).getSelectedObjects()	== null)
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
		
		ArrayList tUnits = new ArrayList();
		// CYCLE THRO THE CHOSEN TABLES
		seenTable = false;
		for (int i = 0; i < potentialDeeperTables.length; i++){
				
				if (potentialDeeperTables[i].getName().equals(refTableName) || 
					(potentialDeeperTables[i].getName().equals(centralTableName) && seenTable))
						 continue;
			
				if (potentialDeeperTables[i].getName().equals(centralTableName))
					seenTable = true;		



				// IF NOT CHECKED CONTINUE
				if (includeCheckBoxs[i].getSelectedObjects() == null)
					continue;
							
				String thisTableName = potentialDeeperTables[i].getName();
		
				// CREATE A TUNIT FOR THE EXTRA TABLE
				Integer tunitCount = new Integer(unitCount + 1);
				Table refTab = potentialDeeperTables[i];
				String cardinality = cardinalityComboBoxs[i].getSelectedItem().toString();
				String centralExtension = "";
				if (!deepCenTextFields[i].getText().equals(""))
					centralExtension = deepCenColumnOptions[i].getSelectedItem().toString() + 
						deepCenOperatorOptions[i].getSelectedItem().toString() +
						deepCenTextFields[i].getText();
				String referencedExtension = "";
				if (!deepTextFields[i].getText().equals(""))	
					referencedExtension = deepColumnOptions[i].getSelectedItem().toString() + 
								deepOperatorOptions[i].getSelectedItem().toString() +
								deepTextFields[i].getText();
				
				String refColName = "";
				String refColAlias = "";
				if (refColNames.get(refTab.getName()) != null)
					refColName = (String) refColNames.get(refTab.getName());
				if (refColAliases.get(refTab.getName()) != null)
					refColAlias = (String) refColAliases.get(refTab.getName());
				
							
				TransformationUnit deeperUnit =
							new TransformationUnit(
								tunitCount.toString(),
								refTab.status,
								refTab.PK,//?
								refTab.getName(),
								//centralTableName,// call by central table name ?
								cardinality,
								centralExtension,
								referencedExtension,
								refTab.FK,//?
								refColName,
								refColAlias,
								cenColName,
								cenColAlias,
								"");
				
				tUnits.add(deeperUnit);
				unitCount++;
				// IF GO DEEPER SET ON THIS RECURSIVELY CALL
				if (deepGoDeeperCheckBoxs[i].getSelectedObjects() != null){
						TransformationUnit[] recursiveUnits = getDeeperUnits(thisTableName, refTableName, unitCount);
						for (int k = 0; k < recursiveUnits.length; k++){
							tUnits.add(recursiveUnits[k]);
							unitCount++;
						}
				}
		}
		// return the final array
		TransformationUnit[] deeperUnits = new TransformationUnit[tUnits.size()];
		tUnits.toArray(deeperUnits);		
		return deeperUnits;
	}
	

	public void itemStateChanged(ItemEvent e){
		if (e.getSource().equals(tableOptions)) {
			pK.removeAllItems();
			fK.removeAllItems();
			userRefCol.removeAllItems();
			String addedTableName = (String) tableOptions.getSelectedItem();
			
			
			if(addedTableName !=null){
			
			Column[] tableCols = resolver.getCentralTable(addedTableName).getColumns();
			for (int j = 0; j < tableCols.length; j++) {
				pK.addItem(tableCols[j].getName());
				fK.addItem(tableCols[j].getName());
				userRefCol.addItem(tableCols[j].getName());
			}
		}
			
		}
		else if (e.getSource().equals(extSchema)){
			
			tableOptions.removeAllItems();
			String[] tableNames = resolver.getAllTableNamesBySchema((String) extSchema.getSelectedItem());
			for (int j = 0; j < tableNames.length; j++) {
				tableOptions.addItem(tableNames[j]);
			}
			pK.removeAllItems();
			fK.removeAllItems();
			userRefCol.removeAllItems();
			String addedTableName = (String) tableOptions.getSelectedItem();
			Column[] tableCols = resolver.getCentralTable(addedTableName).getColumns();
			for (int j = 0; j < tableCols.length; j++) {
				pK.addItem(tableCols[j].getName());
				fK.addItem(tableCols[j].getName());
				userRefCol.addItem(tableCols[j].getName());
			}
		}
		else if (e.getSource().equals(dmTableNameBox)){
			ArrayList allCols = new ArrayList();
			Table[] referencedTables =
				resolver.getReferencedTables((String) dmTableNameBox.getSelectedItem());
			int centralSeen = 0;
			for (int k = 0; k < referencedTables.length; k++) {
				if (centralSeen > 0 && referencedTables[k].getName().equals((String) 
						dmTableNameBox.getSelectedItem()))
					continue;
				Column[] tableCols = referencedTables[k].getColumns();
				for (int l = 0; l < tableCols.length; l++) {
					String entry = referencedTables[k].getName()+"."+ tableCols[l].getName();
					allCols.add(entry);
				}
				if (referencedTables[k].getName().equals((String) dmTableNameBox.getSelectedItem()))
					centralSeen++;
			}
			String[] cols = new String[allCols.size()];	
			allCols.toArray(cols);
			partitionColsOption.removeAllItems();
			for (int j = 0; j < cols.length; j++){
				partitionColsOption.addItem(cols[j]);
			}
			
		}
		else if (e.getSource().equals(tableNameBox)){
			ArrayList allCols = new ArrayList();
			Table[] referencedTables =
						resolver.getReferencedTables((String) tableNameBox.getSelectedItem());
			int centralSeen = 0;
			for (int k = 0; k < referencedTables.length; k++) {
			if (centralSeen > 0 && referencedTables[k].getName().equals((String)tableNameBox.getSelectedItem()))
					continue;
			Column[] tableCols = referencedTables[k].getColumns();
			for (int l = 0; l < tableCols.length; l++) {
				String entry = referencedTables[k].getName()+"."+ tableCols[l].getName();
				allCols.add(entry);
			}
			if (referencedTables[k].getName().equals((String) tableNameBox.getSelectedItem()))
				centralSeen++;
			}
			String[] cols = new String[allCols.size()];	
			allCols.toArray(cols);
			partitionColsOption.removeAllItems();
			for (int j = 0; j < cols.length; j++){
				partitionColsOption.addItem(cols[j]);
			}
		}
	}
		
}
