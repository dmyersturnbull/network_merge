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

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;



public class NetworkCombinerTest {

	private static final String RESOURCE_DIR = "src/test/resources/util/";

	@Test
	public void test() {
		File input1 = new File(RESOURCE_DIR + "uncombined_input_1.psimi.xml");
		File input2 = new File(RESOURCE_DIR + "uncombined_input_2.psimi.xml");
		File expected = new File(RESOURCE_DIR + "combined_output.psimi.xml");
		File output = new File("combinedoutput.psimi.xml.tmp");
		output.deleteOnExit();
		NetworkCombiner combiner = new NetworkCombiner(1);
		combiner.combine(output, input1, input2);
		boolean similar = TestUtils.compareXml(expected, output);
		assertTrue("XML differs from expected", similar);
	}
	
}
