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

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.ensembl.mart.lib.config.DatasetView;

/**
 * @author craig
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class DatasetViewTree extends PopUpTreeCombo {

	private AdaptorManager manager;

	public DatasetViewTree(AdaptorManager manager) {
		super("DatasetView");
    this.manager = manager;
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.explorer.PopUpTreeCombo#update()
	 */
	public void update() {
//		// TODO Auto-generated method stub
//    DatasetView[] datasetViews = manager.getRootAdaptor().getDatasetViews();
//    
//    if (datasetViews == null || datasetViews.length == 0)
//      return;
//
//    //  we need the dsvs sorted so we can construct the menu tree
//    // by parsing the array once
//    Arrays.sort(datasetViews, new Comparator() {
//      public int compare(Object o1, Object o2) {
//        DatasetView d1 = (DatasetView) o1;
//        DatasetView d2 = (DatasetView) o2;
//        return d1.getDisplayName().compareTo(d2.getDisplayName());
//      }
//    });
//
//    String[][] tree = new String[][] {
//    };
//    Map menus = new HashMap();
//
//    for (int i = 0; i < datasetViews.length; i++) {
//      final DatasetView view = datasetViews[i];
//
//      final String datasetName = view.getDisplayName();
//
//      String[] elements = datasetName.split("__");
//
//      for (int j = 0; j < elements.length; j++) {
//
//        String substring = elements[j];
//
//        JMenu parent = treeTopMenu;
//        if (j > 0)
//          parent = (JMenu) menus.get(elements[j - 1]);
//
//        if (j + 1 == elements.length) {
//
//          // user selectable leaf node
//          JMenuItem item = new JMenuItem(substring);
//          item.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent event) {
//              doSelect(view.getDisplayName());
//            }
//          });
//          parent.add(item);
//
//        } else {
//
//          // intermediate menu node
//          JMenu menu = (JMenu) menus.get(elements[j]);
//          if (menu == null) {
//            menu = new JMenu(substring);
//            menus.put(substring, menu);
//            parent.add(menu);
//          }
//
//        }
//
//      }
//    }
	}

	public static void main(String[] args) {
	}
}
