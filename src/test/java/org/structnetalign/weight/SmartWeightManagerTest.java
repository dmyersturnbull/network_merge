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
package org.structnetalign.weight;

import static org.junit.Assert.*;

import org.junit.Test;
import org.structnetalign.HomologyEdge;

import edu.uci.ics.jung.graph.UndirectedGraph;



public class SmartWeightManagerTest {

	private static final double PRECISION = 0.001;
	
	@Test
	public void test() {
		SmartWeightManager manager = new SmartWeightManager(2);
		UndirectedGraph<Integer,HomologyEdge> hom = WeightManagerTest.testSimple(manager);
		assertEquals("Wrong number of homology edges", 15, hom.getEdgeCount());
		
		// gets an alignment score of 0.507 and a SCOP score of 0.4
		// note that we don't know the Id in a multithreaded environment
		assertEquals(0.6309827640820483, hom.findEdge(4, 5).getWeight(), PRECISION);
	}
	
}
