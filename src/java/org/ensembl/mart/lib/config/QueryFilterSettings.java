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

package org.ensembl.mart.lib.config;


/**
 * @author craig
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public abstract class QueryFilterSettings extends BaseNamedConfigurationObject {

	protected final String fieldKey = "field";
	protected final String valueKey = "value";
	protected final String handlerKey = "handler";
	protected final String tableConstraintKey = "tableConstraint";
	protected final String typeKey = "type";
	protected final String qualifierKey = "qualifier";
	protected final String legalQualifiersKey = "legal_qualifiers";

	/**
	 * Copy constructor.  Creates an exact copy of an existing object.
	 * @param bo - BaseNamedConfigurationObject to copy.
	 */
	public QueryFilterSettings(BaseNamedConfigurationObject bo) {
		super(bo);
	}

	/**
	 * Empty Constructor should only be used by DatasetViewEditor
	 *
	 */
	public QueryFilterSettings() {
		super();
	}

  /**
   * Get all Option objects available as an array.  Options are returned in the order they were added.
   * @return Option[]
   */
  public abstract Option[] getOptions();


	/**
	 * @param internalName
	 * @param displayName
	 * @param description
	 * @throws ConfigurationException
	 */
	public QueryFilterSettings(String internalName, String displayName, String description)
		throws ConfigurationException {
			this(internalName, displayName, description, "", "", "", null, "", "", "");
	}

  public QueryFilterSettings(String internalName, String displayName, String description, String field, String value, String tableConstraint, String handler, String type, String qualifier, String legalQualifiers) throws ConfigurationException {
		super(internalName, displayName, description);
		
    setAttribute(fieldKey, field);
    setAttribute(valueKey, value);
		setAttribute(handlerKey, handler);
		setAttribute(tableConstraintKey, tableConstraint);
		setAttribute(typeKey, type);
		setAttribute(qualifierKey, qualifier);
		setAttribute(legalQualifiersKey, legalQualifiers);
  }
  
	public void setField(String field) {
		setAttribute(fieldKey, field);
	}
	
	public String getField() {
		return getAttribute(fieldKey);
	}

	public abstract String getFieldFromContext();

	public void setHandler(String handler) {
		setAttribute(handlerKey, handler);
	}

	public String getHandler() {
		return getAttribute(handlerKey);
	}

	public abstract String getHandlerFromContext();

	public void setTableConstraint(String tableConstraint) {
		setAttribute(tableConstraintKey, tableConstraint);
	}

	public String getTableConstraint() {
		return getAttribute(tableConstraintKey);
	}

	public abstract String getTableConstraintFromContext();

	public void setType(String type) {
		setAttribute(typeKey, type);
	}

	public String getType() {
		return getAttribute(typeKey);
	}

	public abstract String getTypeFromContext();

	public void setQualifier(String qualifier) {
		setAttribute(qualifierKey, qualifier);
	}

	public String getQualifier() {
		return getAttribute(qualifierKey);
	}

	public abstract String getQualifierFromContext();

	public void setLegalQualifiers(String legalQualifiers) {
		setAttribute(legalQualifiersKey, legalQualifiers);
	}

	public String getLegalQualifiers() {
		return getAttribute(legalQualifiersKey);
	}
	public abstract String getLegalQualifiersFromContext();

  public abstract String getValueFromContext();
  
	public boolean supports(String field, String tableConstraint) {
		boolean supports = false;
		
		//if field is null, this Object cannot support any field x tableConstraint combination 
		if (getAttribute(fieldKey) != null) {
				supports = getAttribute(fieldKey).equals(field);
				
				if (supports) {
					String tableConstraintK = getAttribute(tableConstraintKey);
					
					//if the field matches, it only depends on the tableConstraints if the given or the object has a non null tableConstraint
					if (tableConstraint != null || tableConstraintK != null) {
						supports = tableConstraintK != null && tableConstraint != null && tableConstraintK.equals(tableConstraint);
					}
				}
		}
		
		return supports;
	}

	protected boolean hasBrokenField = false;
	protected boolean hasBrokenTableConstraint = false;
	/**
		 * set the hasBrokenField flag to true, eg. the field
		 * does not refer to an existing field in a particular Mart Dataset instance.
		 *
		 */
	public void setFieldBroken() {
		hasBrokenField = true;
	}

	/**
		 * Determine if this Option has a broken field reference.
		 * @return boolean, true if field is broken, false otherwise
		 */
	public boolean hasBrokenField() {
		return hasBrokenField;
	}

	/**
		 * set the hasBrokenTableConstraint flag to true, eg. the tableConstraint
		 * does not refer to an existing table in a particular Mart Dataset instance.
		 *
		 */
	public void setTableConstraintBroken() {
		hasBrokenTableConstraint = true;
	}

	/**
		 * Determine if this Option has a broken tableConstraint reference.
		 * @return boolean, true if tableConstraint is broken, false otherwise
		 */
	public boolean hasBrokenTableConstraint() {
		return hasBrokenTableConstraint;
	}

	public void setValue(String value) {
		setAttribute(valueKey, value);
	}

	public String getValue() {
		return getAttribute(valueKey);
	}

	/**
	   * Returns specific option matching field and value. Does a deep search through options and push actions.
	   * @param field field in target option
	   * @param value value in target option
	   * @return option if found, otherwise null
	   */
	public Option getOptionByFieldNameValue(String field, String value) {
    
	  Option o = null;
    
	  Option[] os = getOptions();
	  for (int i = 0; o==null && i < os.length; i++) {
			
      Option option = os[i];
      
      String f = option.getFieldFromContext(); 
      String v = option.getValueFromContext();
			
      if ( f!=null && f.equals(field ) && v!=null && v.equals(value) ) {
	      o = option;
      }  

	    if ( o==null ) {
        PushAction[] pas = option.getPushActions();
        for (int j = 0; o==null && i < pas.length; i++) {
          Option[] ospa = pas[i].getOptions();
          for (int k = 0; o==null && k < ospa.length; k++) {
						Option optionpa = ospa[k];
						o = optionpa.getOptionByFieldNameValue( field, value );
					}
        }
        
	    }
    
      if ( o==null ) {
	      o = option.getOptionByFieldNameValue( field, value);
      }		
    }
	  return o;
	}
}
