/*
 * Created on May 28, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */

import java.sql.*;

/**
 * @author craig
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class Tmp {

	public static void main(String[] args) throws Exception {
		//DriverManager.setLogStream(System.out);
		DriverManager.println("message");
		Class.forName("org.gjt.mm.mysql.Driver").newInstance();
		Connection c = DriverManager.getConnection("jdbc:mysql://kaka.sanger.ac.uk/homo_sapiens_core_13_31", "anonymous","");
		
		ResultSet rs = c.createStatement().executeQuery("show databases");
		while (rs.next()) {}
		System.out.println("done");
	}
	
	
}
