/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * 
 * @author dmyersturnbull
 */
package org.structnetalign.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.biojava.bio.structure.scop.BerkeleyScopInstallation;
import org.biojava.bio.structure.scop.RemoteScopInstallation;
import org.biojava.bio.structure.scop.ScopDatabase;
import org.biojava.bio.structure.scop.ScopInstallation;

public class BasicScop {

	private static final Logger logger = LogManager.getLogger("org.structnetalign");

	private static ScopDatabase scop;

	static {
		Properties props = new Properties();
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		InputStream stream = loader.getResourceAsStream("databases.properties");
		try {
			props.load(stream);
		} catch (IOException e) {
			throw new RuntimeException("Couldn't open databases property file", e);
		}
		String scopSource = props.getProperty("scop_source");
		String scopVersion = props.getProperty("scop_version");
		String scopUrl = props.getProperty("scop_url");
		String scopCache = props.getProperty("scop_cache");
		logger.info("Setting SCOP to " + scopSource + " " + scopUrl + " " + scopVersion);
		if (scopSource.equals("cambridge")) {
			ScopInstallation a = new ScopInstallation();
			a.setScopVersion(scopVersion);
			a.setScopDownloadURL(scopUrl);
			if (scopCache != null) {
				a.setCacheLocation(scopCache);
			}
			scop = a;
		} else if (scopSource.equals("berkeley")) {
			BerkeleyScopInstallation a = new BerkeleyScopInstallation();
			a.setScopVersion(scopVersion);
			a.setScopDownloadURL(scopUrl);
			if (scopCache != null) {
				a.setCacheLocation(scopCache);
			}
			scop = a;
		} else if (scopSource.equals("remote")) {
			RemoteScopInstallation a = new RemoteScopInstallation();
			a.setServer(scopUrl);
			scop = a;
		} else {
			throw new IllegalArgumentException("Didn't understand scop_source " + scopSource);
		}
	}

	public static ScopDatabase getScop() {
		return scop;
	}

}
