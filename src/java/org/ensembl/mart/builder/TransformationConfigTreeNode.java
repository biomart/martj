/*
	Copyright (C) 2003 EBI, GRL

	This library is free software; you can redistribute it and/or
	modify it under the terms of the GNU Lesser General Public
	License as published by the Free Software Foundation; either
	version 2.1 of the License, or (at your option) any later version.

	This library is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
	Lesser General Public License for more details.d

	You should have received a copy of the GNU Lesser General Public
	License along with this library; if not, write to the Free Software
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.ensembl.mart.builder;

import java.util.List;
import javax.swing.tree.DefaultMutableTreeNode;

import org.ensembl.mart.builder.config.*;


 
/**
 * Class TransformationConfigTreeNode extends DefaultMutableTreeNode.
 *
 * <p>This class is written so that the tree node is aware of the datasetconfig etc objects
 * </p>
 *
 * @author <a href="mailto:damian@ebi.ac.uk">Damian Smedley</a>
 * //@see org.ensembl.mart.config.TransformationConfig
 */

public class TransformationConfigTreeNode extends DefaultMutableTreeNode {

  /**
   * Each Node stores all child objects in a single Vector, but some TransformationConfigTree
   * userObjects store heterogenous groups of children in different lists in a particular 
   * order. This method calculates the index adjustment to apply to any node Vector index 
   * to get the TransformationConfigTree userObject index, which could be 0 for any TransformationConfigTree
   * object that only stores one type of child object.  In general, for a TransformationConfigTree
   * userObject which stores separate lists of objects a, b, and c in order, the relationship
   * between the node Vector index (V) and the individual object index within the
   * parent TransformationConfigTree userObject is:
   * (a) V[i] = a[i]
   * (b) V[i] = b[i - (a.length)]
   * (c) V[i] = c[i - (a.length + b.length)] 
   * @param parent - TransformationConfigTree userObject into which dropnode is to be dropped 
   * @param child - TransformationConfigTree userObject object for which an index inside parent is needed 
   * @return adjustment for any index of child inside parent.
   */
  	protected static int getHeterogenousOffset(Object parent, Object child) {
    	return 0;
  	}
  
	protected String name;

	public TransformationConfigTreeNode(String name) {
		this.name = name;
	}

	public TransformationConfigTreeNode(String name, Object obj) {
		this.name = name;
		this.setUserObject(obj);
	}

	public void setName(String newName) {
		name = newName;
	}

	public String toString() {
		return name;
	}

	public void setUserObject(Object obj) {
		//System.out.println("ADDING TEST " + obj);
		super.setUserObject(obj);

		String nodeObjectClass = obj.getClass().getName();
		if (nodeObjectClass.equals("org.ensembl.mart.builder.config.TransformationConfig")) {
			setName("TransformationConfig: " + ((BaseNamedConfigurationObject) obj).getInternalName());
			TransformationConfig dsv = (TransformationConfig) obj;
			System.out.println("NODE: 1");
			Dataset[] fpages = dsv.getDatasets();
			for (int i = 0; i < fpages.length; i++) {
				if (fpages[i].getClass().getName().equals("org.ensembl.mart.builder.config.Dataset")) {
					Dataset fp = fpages[i];
					System.out.println("DATASET -> " + fp.toString());
					String fpName = fp.getInternalName();
					System.out.println("NODE: 2");
					TransformationConfigTreeNode fpNode = new TransformationConfigTreeNode("Dataset:" + fpName);
					fpNode.setUserObject(fp);

					this.add(fpNode);
					Transformation[] groups = fp.getTransformations();
					for (int j = 0; j < groups.length; j++) {
						if (groups[j].getClass().getName().equals("org.ensembl.mart.builder.config.Transformation")) {
							Transformation fiGroup = (Transformation) groups[j];
							String grName = fiGroup.getInternalName();
							TransformationConfigTreeNode grNode = new TransformationConfigTreeNode("Transformation:" + grName);
							grNode.setUserObject(fiGroup);
							//this.add(grNode);
							List tunits = fiGroup.getTransformationUnits();
							for (int k = 0; k < tunits.size(); k++) {
								if (tunits.get(k).getClass().getName().equals("org.ensembl.mart.builder.config.TransformationUnit")) {
									TransformationUnit tunit = (TransformationUnit) tunits.get(k);
									String tuName = tunit.getInternalName();
									TransformationConfigTreeNode tuNode = new TransformationConfigTreeNode("TransformationUnit:" + tuName);
									tuNode.setUserObject(tunit);							
							
								}
							}
						}
					}
				}
			}
							
		} else if (nodeObjectClass.equals("org.ensembl.mart.builder.config.Dataset")) {
			setName("Dataset: " + ((BaseNamedConfigurationObject) obj).getInternalName());
			Dataset fp = (Dataset) obj;
			Transformation[] groups = fp.getTransformations();
			for (int j = 0; j < groups.length; j++) {
				if (groups[j].getClass().getName().equals("org.ensembl.mart.builder.config.Transformation")) {
					Transformation fiGroup = (Transformation) groups[j];
					String grName = fiGroup.getInternalName();
					TransformationConfigTreeNode grNode = new TransformationConfigTreeNode("Transformation:" + grName);
					grNode.setUserObject(fiGroup);
					this.add(grNode);
					List tunits = fiGroup.getTransformationUnits();
					for (int k = 0; k < tunits.size(); k++) {
						if (tunits.get(k).getClass().getName().equals("org.ensembl.mart.builder.config.TransformationUnit")) {
							TransformationUnit tunit = (TransformationUnit) tunits.get(k);
							String tuName = tunit.getInternalName();
							TransformationConfigTreeNode tuNode = new TransformationConfigTreeNode("TransformationUnit:" + tuName);
							tuNode.setUserObject(tunit);							
						}
					}
					
					
				}
			}

		} else if (nodeObjectClass.equals("org.ensembl.mart.builder.config.Transformation")) {
			setName("Transformation: " + ((BaseNamedConfigurationObject) obj).getInternalName());
			Transformation fp = (Transformation) obj;
			List groups = fp.getTransformationUnits();
			for (int j = 0; j < groups.size(); j++) {
				if (groups.get(j).getClass().getName().equals("org.ensembl.mart.builder.config.TransformationUnit")) {
					TransformationUnit fiGroup = (TransformationUnit) groups.get(j);
					String grName = fiGroup.getInternalName();
					TransformationConfigTreeNode grNode = new TransformationConfigTreeNode("TransformationUnit:" + grName);
					grNode.setUserObject(fiGroup);
					this.add(grNode);	
				}
			}	

		} else if (nodeObjectClass.equals("org.ensembl.mart.builder.config.TransformationUnit")) {
			setName("TransformationUnit: " + ((BaseNamedConfigurationObject) obj).getInternalName());
		} 

	}
	
}


