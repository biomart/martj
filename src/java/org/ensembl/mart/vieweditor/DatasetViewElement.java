package org.ensembl.mart.vieweditor;

import org.ensembl.mart.lib.config.DatasetView;

import java.util.StringTokenizer;

/**
 * Created by IntelliJ IDEA.
 * User: Sony
 * Date: 16-Nov-2003
 * Time: 12:32:45
 * To change this template use Options | File Templates.
 */
public class DatasetViewElement {

    protected String fullname;
    protected DatasetView dsView;
    protected String name;
    protected String pageType;
    protected String pageName;
    protected String groupName;
    protected String collectionName;
    protected String descriptionName;

    public DatasetViewElement(String fullname, DatasetView dsView) {
        this.fullname = fullname;
        this.dsView = dsView;
        StringTokenizer st = new StringTokenizer(fullname,":");
        pageType = st.nextToken();
        name = new String();
        while(st.hasMoreElements()){
            name = st.nextToken();
        }
    }

    public String getName(){
        return name;
    }

    public String getFullname(){
        return fullname;
    }

    public boolean isFolder(){
        StringTokenizer st = new StringTokenizer(fullname,":");
        if(st.countTokens() == 5)
            return false;

        return true;
    }
}
