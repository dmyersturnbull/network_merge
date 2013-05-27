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
package org.structnetalign.cross;

import java.io.File;

import org.junit.Test;



public class SimpleCrossingManagerTest {

	private static final String RESOURCE_DIR = "src/test/resources/cross/";
	
	@Test
	public void test() {
		File homologyInput = new File(RESOURCE_DIR + "trivial_hom.graphml.xml");
		File interactionInput = new File(RESOURCE_DIR + "trivial_int.graphml.xml");
		File homologyOutput = new File(RESOURCE_DIR + "trivial_hom_crossed.graphml.xml");
		File interactionOutput = new File(RESOURCE_DIR + "trivial_int_crossed.graphml.xml");
		CrossingManager crosser = new SimpleCrossingManager(2, 10000);
		CrossingManagerTest.test(homologyInput, interactionInput, homologyOutput, interactionOutput, 6, 5, 3, crosser);
	}
	
}
