/*
	Copyright (C) 2003 EBI, GRL

	This library is free software; you can redistribute it and/or
	modify it under the terms of the GNU Lesser General Public
	License as published by the Free Software Foundation; either
	version 2.1 of the License, or (at your option) any later version.

	This library is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
	Lesser General Public License for more details.

	You should have received a copy of the GNU Lesser General Public
	License along with this library; if not, write to the Free Software
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.ensembl.mart.editor;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.ensembl.mart.lib.config.AttributeCollection;
import org.ensembl.mart.lib.config.AttributeDescription;
import org.ensembl.mart.lib.config.AttributeGroup;
import org.ensembl.mart.lib.config.AttributeList;
import org.ensembl.mart.lib.config.AttributePage;
import org.ensembl.mart.lib.config.BaseConfigurationObject;
import org.ensembl.mart.lib.config.BaseNamedConfigurationObject;
import org.ensembl.mart.lib.config.DatasetConfig;
import org.ensembl.mart.lib.config.DynamicAttributeContent;
import org.ensembl.mart.lib.config.DynamicDatasetContent;
import org.ensembl.mart.lib.config.DynamicExportableContent;
import org.ensembl.mart.lib.config.DynamicFilterContent;
import org.ensembl.mart.lib.config.DynamicImportableContent;
import org.ensembl.mart.lib.config.Exportable;
import org.ensembl.mart.lib.config.FilterCollection;
import org.ensembl.mart.lib.config.FilterDescription;
import org.ensembl.mart.lib.config.FilterGroup;
import org.ensembl.mart.lib.config.FilterPage;
import org.ensembl.mart.lib.config.Importable;
import org.ensembl.mart.lib.config.Option;
import org.ensembl.mart.lib.config.PushAction;

/**
 * Class DatasetConfigAttributeTableModel implementing TableModel.
 *
 * <p>This class is written for the attributes table to implement autoscroll
 * </p>
 *
 * @author <a href="mailto:katerina@ebi.ac.uk">Katerina Tzouvara</a>
 * //@see org.ensembl.mart.config.DatasetConfig
 */

public class DatasetConfigAttributeTableModel implements TableModel {

	protected String[] columnNames = { "Attribute", "Value" };
	protected Vector tableModelListenerList;
	protected static final int COLUMN_COUNT = 2;
	protected BaseConfigurationObject obj;
	protected String objClass;
	protected String[] firstColumnData;
	protected DatasetConfigTreeNode node;
	protected DatasetConfigTreeNode parent;
	protected int[] requiredFields;

	public DatasetConfigAttributeTableModel(DatasetConfigTreeNode node, String[] firstColumnData, String objClass) {
		this.node = node;
		this.obj = (BaseConfigurationObject) node.getUserObject();
		this.firstColumnData = firstColumnData;
		this.objClass = objClass;
		tableModelListenerList = new Vector();
		parent = (DatasetConfigTreeNode) node.getParent();
		
		requiredFields = obj.getRequiredFields();
	}

	public void addTableModelListener(TableModelListener l) {
		// Adds a listener to the list that is notified each time a change to the data model occurs.
		tableModelListenerList.add(l);
	}

	public Class getColumnClass(int columnIndex) {
		//Returns the most specific superclass for all the cell values in the column.
		try {
			return Class.forName("java.lang.String");
		} catch (Exception e) {
			return null;
		}
	}

	public int getColumnCount() {
		//Returns the number of columns in the model.
		return COLUMN_COUNT;
	}
	
	public int[] getRequiredFields() {
		return requiredFields;
	}

	public String getColumnName(int columnIndex) {
		//Returns the name of the column at columnIndex.
		return columnNames[columnIndex];
	}

	public int getRowCount() {
		//Returns the number of rows in the model.
		return firstColumnData.length;
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		//Returns the value for the cell at columnIndex and rowIndex.
		if (columnIndex == 0) {
			return firstColumnData[rowIndex];
		} else {
			return obj.getAttribute(firstColumnData[rowIndex]);
		}
	}

	public boolean isCellEditable(int rowIndex, int columnIndex) {
		//Returns true if the cell at rowIndex and columnIndex is editable.
		if (columnIndex == 0)
			return false;
		return true;
	}

	public void removeTableModelListener(TableModelListener l) {
		//Removes a listener from the list that is notified each time a change to the data model occurs.
		while (tableModelListenerList.remove((Object) l));
	}

	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {		
		//Sets the value in the cell at columnIndex and rowIndex to aValue.
		Object child = node.getUserObject();
		
		if (((BaseNamedConfigurationObject)child).getTemplateDrivenFlag() == 1){
			JOptionPane.showMessageDialog(null,"Read only - import a configuration to edit");
			return;
		}
				
		//if (((String) aValue).equals("MULTI") ){
		if (obj.getAttribute(firstColumnData[rowIndex]) != null && ((String) obj.getAttribute(firstColumnData[rowIndex])).equals("MULTI")){
			JOptionPane.showMessageDialog(null,"Edit the dynamic nodes for each dataset to set this value");
			//return;	
		}
		
		DatasetConfigTreeNode rootNode = (DatasetConfigTreeNode) node.getRoot();
		DatasetConfig dsConfig = (DatasetConfig) rootNode.getUserObject();		
		
		if (columnIndex == 1) {
			//child may be a DatasetConfig, in which case dont try to remove/add the child to a null parent
			if (child instanceof org.ensembl.mart.lib.config.DatasetConfig) {
				
				if (rowIndex == 0){
					//System.out.println("SHOULD NOT EDIT INTERNAL NAME");
					JOptionPane.showMessageDialog(null,"SHOULD NOT EDIT INTERNAL NAME");
				}
				else if (rowIndex == 10){
					JOptionPane.showMessageDialog(null,"SHOULD NOT EDIT INTERNAL DATASET ID");
				}		  
				else {
					obj.setAttribute(firstColumnData[rowIndex], (String) aValue);
				}
			} else {
				Object parent = ((DatasetConfigTreeNode) node.getParent()).getUserObject();
				int index = node.getParent().getIndex(node) - DatasetConfigTreeNode.getHeterogenousOffset(parent, child);

				if (parent instanceof org.ensembl.mart.lib.config.DatasetConfig) {
					DatasetConfig config = (DatasetConfig) ((DatasetConfigTreeNode) node.getParent()).getUserObject();

					if (child instanceof org.ensembl.mart.lib.config.Importable){
						config.removeImportable((Importable) node.getUserObject());
					}
					else if (child instanceof Exportable){
						config.removeExportable((Exportable) node.getUserObject());
					}
					else if (child instanceof org.ensembl.mart.lib.config.FilterPage){
						config.removeFilterPage((FilterPage) node.getUserObject());
					}
					else if (child instanceof org.ensembl.mart.lib.config.AttributePage){
						config.removeAttributePage((AttributePage) node.getUserObject());
					}
					else if (child instanceof DynamicDatasetContent){
						config.removeDynamicDatasetContent((DynamicDatasetContent) node.getUserObject());
					}
				} else if (parent instanceof org.ensembl.mart.lib.config.Importable) {
						Importable imp = (Importable) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
						if (child instanceof DynamicImportableContent){
							imp.removeDynamicImportableContent((DynamicImportableContent) node.getUserObject());
						}
				} else if (parent instanceof org.ensembl.mart.lib.config.Exportable) {
						Exportable exp = (Exportable) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
						if (child instanceof DynamicExportableContent){
							exp.removeDynamicExportableContent((DynamicExportableContent) node.getUserObject());
						}
				} else if (parent instanceof org.ensembl.mart.lib.config.FilterPage) {
					FilterPage fp = (FilterPage) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
					if (child instanceof org.ensembl.mart.lib.config.FilterGroup){
						fp.removeFilterGroup((FilterGroup) node.getUserObject());
					}
				} else if (parent instanceof org.ensembl.mart.lib.config.FilterGroup) {
					FilterGroup fg = (FilterGroup) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
					if (child instanceof org.ensembl.mart.lib.config.FilterCollection){
						fg.removeFilterCollection((FilterCollection) node.getUserObject());
					}
				} else if (parent instanceof org.ensembl.mart.lib.config.FilterCollection) {
					FilterCollection fc = (FilterCollection) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
					if (child instanceof org.ensembl.mart.lib.config.FilterDescription){
						if (checkFilterUniqueness((String) aValue, rowIndex, dsConfig)){				
								fc.removeFilterDescription((FilterDescription) node.getUserObject());
						}
						else{
							String newName = JOptionPane.showInputDialog("This internal name is duplicated. Choose another");
							fc.removeFilterDescription((FilterDescription) node.getUserObject());
							setValueAt(newName,rowIndex,columnIndex);
							return;
						}
					}
				} else if (parent instanceof FilterDescription) {
					FilterDescription fdesc = (FilterDescription) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
					if (child instanceof Option){
						
						//if (checkOptionUniqueness((String) aValue, dsConfig)){	
							fdesc.removeOption((Option) node.getUserObject());			
						//}
						//else{
						//	String newName = JOptionPane.showInputDialog("This internal name is duplicated. Choose another");
						//	fdesc.removeOption((Option) node.getUserObject());
						//	setValueAt(newName,rowIndex,columnIndex);
						//	return;
						//}	
					} else if (child instanceof org.ensembl.mart.lib.config.DynamicFilterContent){
						fdesc.removeDynamicFilterContent((DynamicFilterContent) node.getUserObject());
				    }
				} else if (parent instanceof DynamicFilterContent){
					DynamicFilterContent fdesc = (DynamicFilterContent) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
					if (child instanceof Option){						
						fdesc.removeOption((Option) node.getUserObject());			
					}
						
				} else if (parent instanceof Option) {
					Option op = (Option) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
					if (child instanceof Option)
						op.removeOption((Option) node.getUserObject());
					else if (child instanceof PushAction)
						op.removePushAction((PushAction) node.getUserObject());
				} else if (parent instanceof PushAction) {
					PushAction pa = (PushAction) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
					if (child instanceof Option)
						pa.removeOption((Option) node.getUserObject());
				} else if (parent instanceof org.ensembl.mart.lib.config.AttributePage) {
					AttributePage ap = (AttributePage) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
					if (child instanceof org.ensembl.mart.lib.config.AttributeGroup){
						ap.removeAttributeGroup((AttributeGroup) node.getUserObject());
					}
				} else if (parent instanceof org.ensembl.mart.lib.config.AttributeGroup) {
					AttributeGroup ag = (AttributeGroup) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
					if (child instanceof org.ensembl.mart.lib.config.AttributeCollection){
						ag.removeAttributeCollection((AttributeCollection) node.getUserObject());
					}
				} else if (parent instanceof org.ensembl.mart.lib.config.AttributeCollection) {
					AttributeCollection ac = (AttributeCollection) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
					if (child instanceof org.ensembl.mart.lib.config.AttributeDescription){
						if (checkUniqueness((String) aValue, rowIndex, dsConfig)){				
							ac.removeAttributeDescription((AttributeDescription) node.getUserObject());
						}
						else{
							//new Exception().printStackTrace();// good debugging tool
							String newName = JOptionPane.showInputDialog("This internal name is duplicated. Choose another");
							ac.removeAttributeDescription((AttributeDescription) node.getUserObject());
							setValueAt(newName,rowIndex,columnIndex);
							return;
						}
					}					
					else if (child instanceof org.ensembl.mart.lib.config.AttributeList){
						if (checkUniqueness((String) aValue, rowIndex, dsConfig)){				
							ac.removeAttributeList((AttributeList) node.getUserObject());
						}
						else{
							//new Exception().printStackTrace();// good debugging tool
							String newName = JOptionPane.showInputDialog("This internal name is duplicated. Choose another");
							ac.removeAttributeList((AttributeList) node.getUserObject());
							setValueAt(newName,rowIndex,columnIndex);
							return;
						}
					}
				}  else if (parent instanceof org.ensembl.mart.lib.config.AttributeDescription) {
					AttributeDescription ad = (AttributeDescription) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
					if (child instanceof org.ensembl.mart.lib.config.DynamicAttributeContent){
						ad.removeDynamicAttributeContent((DynamicAttributeContent) node.getUserObject());
					}
				}  

				obj.setAttribute(firstColumnData[rowIndex], (String) aValue);


				if (parent instanceof org.ensembl.mart.lib.config.DatasetConfig) {
					DatasetConfig config = (DatasetConfig) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
					if (child instanceof org.ensembl.mart.lib.config.FilterPage)
						config.insertFilterPage(index, (FilterPage) obj);
					else if (child instanceof org.ensembl.mart.lib.config.AttributePage)
						config.insertAttributePage(index, (AttributePage) obj);
					else if (child instanceof Importable)
						config.insertImportable(index, (Importable) obj);
					else if (child instanceof Exportable)
						config.insertExportable(index, (Exportable) obj);          
					else if (child instanceof DynamicDatasetContent)
						config.insertDynamicDatasetContent(index, (DynamicDatasetContent) obj);          						  
				} else if (parent instanceof Importable) {
					Importable imp = (Importable) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
					if (child instanceof DynamicImportableContent)
						imp.insertDynamicImportableContent(index, (DynamicImportableContent) obj);
				} else if (parent instanceof Exportable) {
					Exportable exp = (Exportable) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
					if (child instanceof DynamicExportableContent)
						exp.insertDynamicExportableContent(index, (DynamicExportableContent) obj);
				} else if (parent instanceof org.ensembl.mart.lib.config.FilterPage) {
					FilterPage fp = (FilterPage) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
					if (child instanceof org.ensembl.mart.lib.config.FilterGroup)
						fp.insertFilterGroup(index, (FilterGroup) obj);
				} else if (parent instanceof org.ensembl.mart.lib.config.FilterGroup) {
					FilterGroup fg = (FilterGroup) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
					if (child instanceof org.ensembl.mart.lib.config.FilterCollection)
						fg.insertFilterCollection(index, (FilterCollection) obj);
				} else if (parent instanceof org.ensembl.mart.lib.config.FilterCollection) {
					FilterCollection fc = (FilterCollection) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
					if (child instanceof org.ensembl.mart.lib.config.FilterDescription)
						fc.insertFilterDescription(index, (FilterDescription) obj);
				} else if (parent instanceof FilterDescription) {
					FilterDescription fdesc = (FilterDescription) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
					if (child instanceof Option)
						fdesc.insertOption(index, (Option) obj);
				    else if (child instanceof org.ensembl.mart.lib.config.DynamicFilterContent)	
						fdesc.insertDynamicFilterContent(index, (DynamicFilterContent) obj);		
				} else if (parent instanceof DynamicFilterContent) {
					DynamicFilterContent dynFilter = (DynamicFilterContent) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
					if (child instanceof Option)
						dynFilter.insertOption(index, (Option) obj);	
				}  else if (parent instanceof Option) {
					Option op = (Option) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
					if (child instanceof Option)
						op.insertOption(index, (Option) obj);
					else if (child instanceof PushAction)
						op.insertPushAction(index, (PushAction) obj);
				} else if (parent instanceof PushAction) {
					PushAction pa = (PushAction) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
					if (child instanceof Option)
						pa.insertOption(index, (Option) obj);
				} else if (parent instanceof org.ensembl.mart.lib.config.AttributePage) {
					AttributePage ap = (AttributePage) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
					if (child instanceof org.ensembl.mart.lib.config.AttributeGroup)
						ap.insertAttributeGroup(index, (AttributeGroup) obj);
				} else if (parent instanceof org.ensembl.mart.lib.config.AttributeGroup) {
					AttributeGroup ag = (AttributeGroup) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
					if (child instanceof org.ensembl.mart.lib.config.AttributeCollection)
						ag.insertAttributeCollection(index, (AttributeCollection) obj);
				} else if (parent instanceof org.ensembl.mart.lib.config.AttributeCollection) {
					AttributeCollection ac = (AttributeCollection) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
					if (child instanceof org.ensembl.mart.lib.config.AttributeDescription){		
						ac.insertAttributeDescription(index, (AttributeDescription) obj);
					} else if (child instanceof org.ensembl.mart.lib.config.AttributeList){		
						ac.insertAttributeList(index, (AttributeList) obj);
					}
				} else if (parent instanceof org.ensembl.mart.lib.config.AttributeDescription) {
					AttributeDescription ad = (AttributeDescription) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
					if (child instanceof org.ensembl.mart.lib.config.DynamicAttributeContent){		
						ad.insertDynamicAttributeContent(index, (DynamicAttributeContent) obj);
					}
				}
			}
			
			DatasetConfigTreeNode newNode = new DatasetConfigTreeNode(obj.getAttribute("internalName"), obj);

			if (parent != null) {
				int index = parent.getIndex(node);
				node.removeFromParent();
				parent.insert(newNode, index);
				node = newNode;
			}
			TableModelEvent tme = new TableModelEvent(this, rowIndex);
			fireEvent(tme);
		}
	}

	private boolean checkUniqueness(String testName, int rowIndex, DatasetConfig dsConfig){
		
	    //only check the internalName, eg. row 0
	    if (rowIndex != 0)
	      return true;
	    
		AttributePage[] apages = dsConfig.getAttributePages();
		AttributePage apage;
	  
		Hashtable descriptionsMap = new Hashtable();// atts should have a unique internal name
		for (int i = 0; i < apages.length; i++){
			  apage = apages[i];
			
			  if ((apage.getHidden() != null) && (apage.getHidden().equals("true"))){
				  continue;
			  }
		    
			  List testAtts = new ArrayList();
			  testAtts = apage.getAllAttributeDescriptions();
			  for (Iterator iter = testAtts.iterator(); iter.hasNext();) {
				  Object testAtt = iter.next();
				  AttributeDescription testAD = (AttributeDescription) testAtt;
				  if ((testAD.getHidden() != null) && (testAD.getHidden().equals("true"))){
					  continue;
				  }
				  descriptionsMap.put(testAD.getInternalName(),"1");
			  }
			  testAtts = apage.getAllAttributeLists();
			  for (Iterator iter = testAtts.iterator(); iter.hasNext();) {
				  Object testAtt = iter.next();
				  AttributeList testAD = (AttributeList) testAtt;
				  if ((testAD.getHidden() != null) && (testAD.getHidden().equals("true"))){
					  continue;
				  }
				  descriptionsMap.put(testAD.getInternalName(),"1");
			  }
		}
		if (descriptionsMap.containsKey(testName)){
			return false;
		}
		return true;
	}
	
	private boolean checkFilterUniqueness(String testName, int rowIndex, DatasetConfig dsConfig){
		
	    //only check the internal id, eg row 0
	    if (rowIndex != 0)
	        return true;
	    
		FilterPage[] apages = dsConfig.getFilterPages();
		FilterPage apage;
	  
		Hashtable descriptionsMap = new Hashtable();// atts should have a unique internal name
		for (int i = 0; i < apages.length; i++){
			  apage = apages[i];
			
			  if ((apage.getHidden() != null) && (apage.getHidden().equals("true"))){
				  continue;
			  }
		    
			  List testAtts = new ArrayList();
			  testAtts = apage.getAllFilterDescriptions();
			  for (Iterator iter = testAtts.iterator(); iter.hasNext();) {
				  Object testAtt = iter.next();
				  FilterDescription testAD = (FilterDescription) testAtt;
				  if ((testAD.getHidden() != null) && (testAD.getHidden().equals("true"))){
					  continue;
				  }
				  descriptionsMap.put(testAD.getInternalName(),"1");
			  }
		}
		if (descriptionsMap.containsKey(testName)){
			return false;
		}
		return true;
	}
	

	public void setObject(BaseConfigurationObject obj) {
		this.obj = obj;
	}

	public DatasetConfigTreeNode getParentNode() {
		return parent;
	}

	private void fireEvent(TableModelEvent tme) {
		for (Enumeration e = tableModelListenerList.elements(); e.hasMoreElements();) {
			TableModelListener tml = (TableModelListener) e.nextElement();
			tml.tableChanged(tme);
		}
	}
}
