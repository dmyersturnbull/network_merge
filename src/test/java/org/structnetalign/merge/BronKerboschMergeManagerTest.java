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
package org.structnetalign.merge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;
import org.structnetalign.CleverGraph;
import org.structnetalign.util.GraphMLAdaptor;
import org.structnetalign.util.TestUtils;



public class BronKerboschMergeManagerTest {

	private static final String RESOURCE_DIR = "src/test/resources/merge/";
	
	@Test
	public void testTrivial() {
		File homologyInput = new File(RESOURCE_DIR + "trivial_hom.graphml.xml");
		File interactionInput = new File(RESOURCE_DIR + "trivial_int.graphml.xml");
		File homologyOutput = new File(RESOURCE_DIR + "trivial_hom_merged.graphml.xml");
		File interactionOutput = new File(RESOURCE_DIR + "trivial_int_merged.graphml.xml");
		CleverGraph graph = GraphMLAdaptor.readGraph(interactionInput, homologyInput);
		assertEquals(6, graph.getVertexCount()); // just a sanity check for the test itself
		assertEquals(5, graph.getHomologyCount()); // just a sanity check for the test itself
		assertEquals(3, graph.getInteractionCount()); // just a sanity check for the test itself
		BronKerboschMergeManager merge = new BronKerboschMergeManager();
		merge.merge(graph);
		boolean inter = TestUtils.compareInteractionGraph(graph.getInteraction(), interactionOutput);
		assertTrue("Interaction graph differs from expected", inter);
		boolean hom = TestUtils.compareHomologyGraph(graph.getHomology(), homologyOutput);
		assertTrue("Homology graph differs from expected", hom);
	}

	@Test
	public void testTricky() {
		File homologyInput = new File(RESOURCE_DIR + "tricky_hom.graphml.xml");
		File interactionInput = new File(RESOURCE_DIR + "tricky_int.graphml.xml");
		File homologyOutput = new File(RESOURCE_DIR + "tricky_hom_merged.graphml.xml");
		File interactionOutput = new File(RESOURCE_DIR + "tricky_int_merged.graphml.xml");
		CleverGraph graph = GraphMLAdaptor.readGraph(interactionInput, homologyInput);
		assertEquals(17, graph.getVertexCount()); // just a sanity check for the test itself
		assertEquals(23, graph.getHomologyCount()); // just a sanity check for the test itself
		assertEquals(12, graph.getInteractionCount()); // just a sanity check for the test itself
		BronKerboschMergeManager merge = new BronKerboschMergeManager();
		merge.merge(graph);
		boolean inter = TestUtils.compareInteractionGraph(graph.getInteraction(), interactionOutput);
		assertTrue("Interaction graph differs from expected", inter);
		boolean hom = TestUtils.compareHomologyGraph(graph.getHomology(), homologyOutput);
		assertTrue("Homology graph differs from expected", hom);
	}
	
	@Test
	public void testOverlapping() {
		File homologyInput = new File(RESOURCE_DIR + "overlapping_hom.graphml.xml");
		File interactionInput = new File(RESOURCE_DIR + "overlapping_int.graphml.xml");
		File homologyOutput = new File(RESOURCE_DIR + "overlapping_hom_merged.graphml.xml");
		File interactionOutput = new File(RESOURCE_DIR + "overlapping_int_merged.graphml.xml");
		CleverGraph graph = GraphMLAdaptor.readGraph(interactionInput, homologyInput);
		assertEquals(9, graph.getVertexCount()); // just a sanity check for the test itself
		assertEquals(12, graph.getHomologyCount()); // just a sanity check for the test itself
		assertEquals(7, graph.getInteractionCount()); // just a sanity check for the test itself
		BronKerboschMergeManager merge = new BronKerboschMergeManager();
		merge.merge(graph);
		boolean inter = TestUtils.compareInteractionGraph(graph.getInteraction(), interactionOutput);
		assertTrue("Interaction graph differs from expected", inter);
		boolean hom = TestUtils.compareHomologyGraph(graph.getHomology(), homologyOutput);
		assertTrue("Homology graph differs from expected", hom);
	}
	
	@Test
	public void testDifferentSized() {
		File homologyInput = new File(RESOURCE_DIR + "different_sized_hom.graphml.xml");
		File interactionInput = new File(RESOURCE_DIR + "different_sized_int.graphml.xml");
		File homologyOutput = new File(RESOURCE_DIR + "different_sized_hom_merged.graphml.xml");
		File interactionOutput = new File(RESOURCE_DIR + "different_sized_int_merged.graphml.xml");
		CleverGraph graph = GraphMLAdaptor.readGraph(interactionInput, homologyInput);
		assertEquals(8, graph.getVertexCount()); // just a sanity check for the test itself
		assertEquals(9, graph.getHomologyCount()); // just a sanity check for the test itself
		assertEquals(6, graph.getInteractionCount()); // just a sanity check for the test itself
		BronKerboschMergeManager merge = new BronKerboschMergeManager();
		merge.merge(graph);
//		GraphMLAdaptor.writeHomologyGraph(graph.getHomology(), new File("ahom.xml.tmp"));
//		GraphMLAdaptor.writeInteractionGraph(graph.getInteraction(), new File("aint.xml.tmp"));
		boolean inter = TestUtils.compareInteractionGraph(graph.getInteraction(), interactionOutput);
		assertTrue("Interaction graph differs from expected", inter);
		boolean hom = TestUtils.compareHomologyGraph(graph.getHomology(), homologyOutput);
		assertTrue("Homology graph differs from expected", hom);
	}
}
