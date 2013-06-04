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

import psidev.psi.mi.xml.model.EntrySet;



public class NetworkTrimmerTest {

	private static final String RESOURCE_DIR = "src/test/resources/util/";

	@Test
	public void test() {
		EntrySet entrySet = NetworkUtils.readNetwork(RESOURCE_DIR + "untrimmed_network.psimi.xml");
		entrySet = NetworkTrimmer.trim(entrySet, 0.5);
		File output = new File("trimmed.psimi.tmp");
//		output.deleteOnExit();
		NetworkUtils.writeNetwork(entrySet, output);
		File expected = new File(RESOURCE_DIR + "trimmed_network.psimi.xml");
		boolean similar = TestUtils.compareXml(expected, output);
		assertTrue("Trimmed network is wrong", similar);
	}
	
}
