/*
 * Copyright (C) 2003 EBI, GRL
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
 * 
 * Created on Dec 7, 2004
 *
 */

package org.ensembl.mart.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import org.ensembl.mart.lib.DetailedDataSource;
import org.ensembl.mart.lib.InputSourceUtil;
import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.MartRegistry;
import org.ensembl.mart.lib.config.MartRegistryXMLUtils;
import org.ensembl.mart.lib.config.RegistryDSConfigAdaptor;

/**
 * @author dlondon@ebi.ac.uk
 */
public class MartRegistryDBTool {

    private static final String FETCH = "-f";
    private static final String LOAD = "-l";
    private static final String NOCOMPRESS = "-nc";
	private static String toolSwitch; //0
	private static String registryPath; //1
	private static String host; //2
	private static String port; //3
	private static String type; //4
	private static String instance; //5
	private static String user; //6
	private static String password; //7
	private static String registryName = null; //8
    private static boolean compress = true; //9

	private static String usage() {
		return "\nusage: MartRegistryDBTool [-l -f] registryPath dbHost dbPort dbType dbInstanceName dbUser dbPass <registryName> <-nc>"
			+ "\n -l loads MartRegistry file at registryPath into the Database"
			+ "\n -f fetches MartRegistry in database into registryPath\n"
            + "\n\n -nc do not compress the XML in the Database (default is to compress) use with -l"
			+ "\n\n-h for this message\n";
	}

	public static void main(String[] args) {
		if (args.length < 8) {
			System.err.println(usage());
			System.exit(1);
		}

		toolSwitch = args[0];
		registryPath = args[1];
		host = args[2];
		port = args[3];
		type = args[4];
		instance = args[5];
		user = args[6];
		password = args[7];

		if (args.length >= 9)
			registryName = args[8];
        if (args.length == 10)
            compress = (args[9].equals(NOCOMPRESS));

		String jdbcClass = DetailedDataSource.getJDBCDriverClassNameFor(type);

		// apply defaults only if both dbtype and jdbcdriver are null
		if (type == null && jdbcClass == null) {
			type = DetailedDataSource.DEFAULTDATABASETYPE;
			jdbcClass = DetailedDataSource.DEFAULTDRIVER;
		}

		String connectionString = DetailedDataSource.connectionURL(type, host, port, instance);

		// use default name
		if (registryName == null)
			registryName = connectionString;

		//use the default poolsize of 10        
		DetailedDataSource dsource =
			new DetailedDataSource(
				type,
				host,
				port,
				instance,
				connectionString,
				user,
				password,
				DetailedDataSource.DEFAULTPOOLSIZE,
				jdbcClass,
				registryName);

		MartRegistry martreg = null;
		if (toolSwitch.equals(LOAD)) {
			try {
				martreg = MartRegistryXMLUtils.XMLStreamToMartRegistry(InputSourceUtil.getStreamForString(registryPath));
				MartRegistryXMLUtils.storeMartRegistryDocumentToDataSource(dsource, MartRegistryXMLUtils.MartRegistryToDocument(martreg), compress);
			} catch (MalformedURLException e) {
				System.err.println("Recieved invalid URL " + registryPath + ": " + e.getMessage() + "\n");
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				System.err.println("File: " + registryPath + " does not exist\n");
				e.printStackTrace();
			} catch (ConfigurationException e) {
				System.err.println("Could not load Registry " + registryPath + ": " + e.getMessage() + "\n");
				e.printStackTrace();
			} catch (IOException e) {
				System.err.println("Could not load Registry " + registryPath + ": " + e.getMessage() + "\n");
				e.printStackTrace();
			}
		} else if (toolSwitch.equals(FETCH)){
			try {
				martreg = MartRegistryXMLUtils.DataSourceToMartRegistry(dsource);
				MartRegistryXMLUtils.MartRegistryToFile(martreg, new File(registryPath));
			} catch (ConfigurationException e) {
               System.err.println("Could not fetch Registry from " + dsource.getName() + ": " + e.getMessage() + "\n");
				e.printStackTrace();
			}
		} else {
          System.err.println("Recieved invalid switch, must be one of -f or -l\n");
          System.exit(1);
		}

		System.err.println("All Complete");
		System.exit(0);
	}
}
