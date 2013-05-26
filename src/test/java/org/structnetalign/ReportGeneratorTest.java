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

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.structnetalign.util.GraphMLAdaptor;



public class ReportGeneratorTest {

	private static final String RESOURCE_DIR = "src/test/resources/merge/";
	
	/**
	 * Just test that it doesn't error.
	 * @throws IOException 
	 */
	@Test
	public void test() throws IOException {
		File dir = new File("reportgeneratortest");
		dir.mkdir();
		dir.deleteOnExit();
		File output = new File("reportgeneratortest/areport.html");
		output.deleteOnExit();
		File interactionFile = new File(RESOURCE_DIR + "trivial_int.graphml.xml");
		File homologyFile = new File(RESOURCE_DIR + "trivial_hom.graphml.xml");
		File interactionFileMerged = new File(RESOURCE_DIR + "trivial_int_merged.graphml.xml");
		File homologyFileMerged = new File(RESOURCE_DIR + "trivial_hom_merged.graphml.xml");
		ReportGenerator generator = new ReportGenerator(output);
		CleverGraph weighted = GraphMLAdaptor.readGraph(interactionFile, homologyFile);
		CleverGraph merged = GraphMLAdaptor.readGraph(interactionFileMerged, homologyFileMerged);
		generator.saveWeighted(weighted);
		generator.saveCrossed(weighted);
		generator.saveMerged(merged);
		generator.write();
		new File("reportgeneratortest/weighted.png").delete();
		new File("reportgeneratortest/crossed.png").delete();
		new File("reportgeneratortest/merged.png").delete();
	}
	
}
