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
package org.ensembl.mart.explorer;
import org.ensembl.mart.lib.Attribute;
import org.ensembl.mart.lib.Filter;
/**
 * Used to create the label on the TreeNode it added to and store optional
 * attribute and filter objects.
 * 
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp </a>
 */
public class TreeNodeData {
	public static final class Type {
    private String label;

		public Type(String label) {
      assert label!=null;
     this.label = label; 
    }
    
		/**
		 * @return Returns the label.
		 */
		public String getLabel() {
			return label;
		}
	};
	public static final Type DATASOURCE = new Type("Datasource");
	public static final Type DATASET = new Type("Dataset");
	public static final Type ATTRIBUTES = new Type("Attributes");
	public static final Type FILTERS = new Type("Filters");
	public static final Type FORMAT = new Type("Format");
    
	public static final TreeNodeData createDataSourceNode() {
		return new TreeNodeData(DATASOURCE, ":", null);
	};
	public static final TreeNodeData createDatasetNode() {
		return new TreeNodeData(DATASET, ":", null);
	};
	public static final TreeNodeData createAttributesNode() {
		return new TreeNodeData(ATTRIBUTES, null, null);
	};
	public static final TreeNodeData createFilterNode() {
		return new TreeNodeData(FILTERS, null, null);
	}
	public static final TreeNodeData createFormatNode() {
		return new TreeNodeData(FORMAT, null, null);
	}
	private Type type;
	private String separator;
	private String rightText;
	private Attribute attribute;
	private Filter filter;
	
	private TreeNodeData(Type type, String separator,
			String rightText, Attribute attribute, Filter filter) {
		this.type = type;
		this.separator = separator;
		this.rightText = rightText;
		this.attribute = attribute;
		this.filter = filter;
	}
	public TreeNodeData(Type type, String separator,
			String rightText, Attribute attribute) {
		this(type, separator, rightText, attribute, null);
	}
	public TreeNodeData(Type type, String separator,
			String rightText, Filter filter) {
		this(type, separator, rightText, null, filter);
	}
	public TreeNodeData(Type type, String separator,
			String rightText) {
		this(type, separator, rightText, null, null);
	}
	/**
	 * Generates a small piece of html that is used to create the "labels" for
	 * tree nodes.
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("<html>");
		buf.append("<b>");
		if (type != null )
			buf.append(type.label);
		if (separator != null)
			buf.append(separator);
		buf.append("</b> ");
		if (rightText != null)
			buf.append(rightText);
		buf.append("</html>");
		return buf.toString();
	}
	/**
	 * @return attribute if set, otherwise null
	 */
	public Attribute getAttribute() {
		return attribute;
	}
	/**
	 * @return filter if set, otherwise null
	 */
	public Filter getFilter() {
		return filter;
	}
	/**
	 * @return
	 */
	public String getLabel() {
		return type.label;
	}
	/**
	 * @return
	 */
	public String getRightText() {
		return rightText;
	}
	/**
	 * @return
	 */
	public String getSeparator() {
		return separator;
	}
	/**
	 * @param string
	 */
	public void setRightText(String string) {
		rightText = string;
	}
	/**
	 * @return
	 */
	public Type getType() {
		return type;
	}
}
