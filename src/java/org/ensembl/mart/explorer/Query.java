package org.ensembl.mart.explorer;

import java.util.*;

public class Query {
  public List getAttributes(){
      return attributes;
    }

  public void setAttributes(List attributes){
      this.attributes = attributes;
    }

  public List getFilters(){
      return filters;
    }

  public void setFilters(List filters){
      this.filters = filters;
    }

  public void addAttribute(Attribute attribute) {
  }

  public void addFilter(Filter filter) {
  }

  private List attributes;
  private List filters;

  /** @link dependency */
  /*# Attribute lnkAttribute; */

  /** @link dependency */
  /*# Filter lnkFilter; */
}
