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
 * @author dmyersturnbull
 */
package org.structnetalign.cross;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;
import org.structnetalign.CleverGraph;
import org.structnetalign.merge.MergeManager;
import org.structnetalign.util.GraphMLAdaptor;
import org.structnetalign.util.TestUtils;

public class CrossingManagerTest {

	public static void test(File homologyInput, File interactionInput, File homologyOutput, File interactionOutput, int nVertices, int nHomologies, int nInteractions, CrossingManager crosser) {
		CleverGraph graph = GraphMLAdaptor.readGraph(interactionInput, homologyInput);
		assertEquals(nVertices, graph.getVertexCount()); // just a sanity check for the test itself
		assertEquals(nHomologies, graph.getHomologyCount()); // just a sanity check for the test itself
		assertEquals(nInteractions, graph.getInteractionCount()); // just a sanity check for the test itself
		crosser.cross(graph);
		GraphMLAdaptor.writeHomologyGraph(graph.getHomology(), new File("ahom.xml.tmp"));
		GraphMLAdaptor.writeInteractionGraph(graph.getInteraction(), new File("aint.xml.tmp"));
		boolean inter = TestUtils.compareInteractionGraph(graph.getInteraction(), interactionOutput);
		assertTrue("Interaction graph differs from expected", inter);
		boolean hom = TestUtils.compareHomologyGraph(graph.getHomology(), homologyOutput);
		assertTrue("Homology graph differs from expected", hom);
	}
	
}
