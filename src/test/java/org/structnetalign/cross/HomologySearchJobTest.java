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

import static org.junit.Assert.*;

import org.junit.Test;
import org.structnetalign.CleverGraph;
import org.structnetalign.HomologyEdge;
import org.structnetalign.InteractionEdge;



public class HomologySearchJobTest {

	private static final double PRECISION = 0.000001;
	
	@Test
	public void testTrivial() throws Exception {
		InteractionEdge rootInteraction = new InteractionEdge(1, 0.4);
		CleverGraph graph = new CleverGraph();
		graph.addVertex(1);
		graph.addVertex(2);
		graph.addVertex(3);
		graph.addVertex(4);
		graph.addVertex(5);
		graph.addVertex(6);
		graph.addInteraction(rootInteraction, 1, 3);
		graph.addInteraction(new InteractionEdge(2, 0.4), 2, 4);
		graph.addInteraction(new InteractionEdge(3, 0.4), 5, 7);
		graph.addHomology(new HomologyEdge(1, 0.8), 1, 2);
		graph.addHomology(new HomologyEdge(2, 0.8), 3, 4);
		graph.addHomology(new HomologyEdge(3, 0.8), 2, 5);
		graph.addHomology(new HomologyEdge(4, 0.8), 4, 7);
		HomologySearchJob job = new HomologySearchJob(rootInteraction, graph);
		job.setMaxDepth(100);
		InteractionEdgeUpdate update = job.call();
		assertEquals("Wrong vertex", 1, update.getInteractorA());
		assertEquals("Wrong vertex", 3, update.getInteractorB());
		assertEquals("Wrong root interaction", rootInteraction, update.getRootInteraction());
		// 0.4 + 0.4⋅0.8^2 − 0.4⋅0.4⋅0.8^2 = 0.5536
		// 0.5536 + 0.8^4⋅0.4 − 0.5536⋅0.8^4⋅0.4 = 0.62673818
		assertEquals("Wrong probability", 0.37789696, update.getScore(), PRECISION); 
	}
	
}
