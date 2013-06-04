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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;
import org.structnetalign.InteractionEdge;

import psidev.psi.mi.xml.model.EntrySet;
import edu.uci.ics.jung.graph.UndirectedGraph;



public class GraphInteractionAdaptorTest {

	private static final String RESOURCE_DIR = "src/test/resources/util/";

	@Test
	public void testToGraphEasy() {
		EntrySet entrySet = NetworkUtils.readNetwork(RESOURCE_DIR + "psimi_input.psimi.xml");
		UndirectedGraph<Integer, InteractionEdge> interaction = GraphInteractionAdaptor.toGraph(entrySet, null, null);
		assertEquals(5, interaction.getVertexCount());
		assertEquals(3, interaction.getEdgeCount());
		File expectedFile = new File(RESOURCE_DIR + "expected_graphml_output.graphml.xml");
		boolean similar = TestUtils.compareInteractionGraph(interaction, expectedFile);
		assertTrue("Graph from PSI-MI is wrong", similar);
	}
	
	@Test
	public void testModifyProbabilities() {
		EntrySet entrySet = NetworkUtils.readNetwork(RESOURCE_DIR + "unmodified_probabilities.psimi.xml");
		UndirectedGraph<Integer, InteractionEdge> graph = GraphInteractionAdaptor.toGraph(entrySet, "struct-NA intial weighting", "struct-NA intial weighting");
		int i = 0;
		for (InteractionEdge e : graph.getEdges()) {
			if (i % 2 == 0) e.setWeight(e.getWeight() + 1); // goes up to 1.36
			i++;
		}
		GraphInteractionAdaptor.modifyProbabilites(entrySet, graph, null);
		File expected = new File(RESOURCE_DIR + "modified_probabilities.psimi.xml");
		File output = new File("modifiedprobs.psimi.tmp");
		output.deleteOnExit();
		NetworkUtils.writeNetwork(entrySet, output);
		assertTrue("Modified graph is wrong", TestUtils.compareXml(expected, output));
	}
	
}
