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
package org.structnetalign;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.structnetalign.util.GraphMLAdaptor;
import org.structnetalign.util.NetworkCombiner;



public class ReportGeneratorTest {

	static final Logger logger = Logger.getLogger(NetworkCombiner.class.getName());

	private static final String RESOURCE_DIR = "src/test/resources/merge/";

	/**
	 * Just test that it creates some files.
	 * @throws IOException 
	 */
	@Test
	public void test() throws IOException {
		File dir = null;
		try {
			dir = new File("reportgeneratortest");
			dir.mkdir();
			File output = new File("reportgeneratortest/areport.html");
			File interactionFile = new File(RESOURCE_DIR + "tricky_int.graphml.xml");
			File homologyFile = new File(RESOURCE_DIR + "tricky_hom.graphml.xml");
			ReportGenerator generator = new ReportGenerator(output);
			CleverGraph weighted = GraphMLAdaptor.readGraph(interactionFile, homologyFile);
			CleverGraph merged = GraphMLAdaptor.readGraph(interactionFile, homologyFile);
			generator.saveWeighted(weighted);
			generator.saveCrossed(weighted);
			generator.saveMerged(merged);
			logger.info("Ignore the following warning Velocity error messages");
			generator.write();
			File weightedImg = new File("reportgeneratortest/weighted.png");
			File crossedImg = new File("reportgeneratortest/crossed.png");
			File mergedImg = new File("reportgeneratortest/merged.png");
			File css = new File("reportgeneratortest/main.css");
			assertTrue("Report file was not created", output.exists());
			assertTrue("Weighted image was not created", weightedImg.exists());
			assertTrue("Crossed image was not created", crossedImg.exists());
			assertTrue("Merged image was not created", mergedImg.exists());
			assertTrue("CSS file was not copied", css.exists());
			output.delete();
			weightedImg.delete();
			crossedImg.delete();
			mergedImg.delete();
			css.delete();
		} finally {
			if (dir != null) dir.delete();
		}
	}

}
