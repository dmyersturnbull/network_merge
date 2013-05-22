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
import java.io.IOException;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Test;
import org.structnetalign.CleverGraph;
import org.structnetalign.HomologyEdge;
import org.structnetalign.InteractionEdge;
import org.xml.sax.SAXException;

import edu.uci.ics.jung.graph.UndirectedGraph;

public class GraphMLAdaptorTest {

	private static final String RESOURCE_DIR = "src/test/resources/util/";

	@Test
	public void testReadGraph() {
		UndirectedGraph<String, String> graph = GraphMLAdaptor.readGraph(RESOURCE_DIR + "agraph.graphml.xml");
		assertEquals(11, graph.getVertexCount());
		assertEquals(8, graph.getEdgeCount());
		String[] vertices = new String[] { "x", "s", "t", "x3", "x4", "v5", "v6", "v7", "v8", "v9", "v10" };
		SortedSet<String> vSet = new TreeSet<>();
		for (String v : vertices)
			vSet.add(v);
		String[] edges = new String[] { "e1", "e2", "e3", "e4", "e5", "e6", "e7", "y_" };
		SortedSet<String> eSet = new TreeSet<>();
		for (String e : edges)
			eSet.add(e);
		assertTrue("Vertices are wrong", CollectionUtils.isEqualCollection(vSet, graph.getVertices()));
		assertTrue("Edges are wrong", CollectionUtils.isEqualCollection(eSet, graph.getEdges()));
	}

	@Test
	public void testReadInterationGraph() throws IOException, SAXException {
		File file = new File(RESOURCE_DIR + "int_1.graphml.xml");
		UndirectedGraph<Integer, InteractionEdge> graph = GraphMLAdaptor.readInteractionGraph(file);
		HashSet<Integer> vertexIds = new HashSet<Integer>();
		vertexIds.add(0);
		vertexIds.add(2);
		vertexIds.add(4);
		vertexIds.add(5);
		assertTrue("Vertices are wrong", CollectionUtils.isEqualCollection(vertexIds, graph.getVertices()));
		HashSet<InteractionEdge> weights = new HashSet<>();
		weights.add(new InteractionEdge(0, 0.1));
		weights.add(new InteractionEdge(1, 0.3));
		weights.add(new InteractionEdge(2, 0.6));
		assertTrue("Edges are wrong", CollectionUtils.isEqualCollection(weights, graph.getEdges()));
	}

	@Test
	public void testWriteHomologyGraph() throws IOException, SAXException {
		CleverGraph clever = new CleverGraph();
		clever.addVertex(0);
		clever.addVertex(2);
		clever.addVertex(3);
		clever.addVertex(4);
		clever.addVertex(5);
		clever.addHomology(new HomologyEdge(0, 0.1), 0, 5);
		clever.addHomology(new HomologyEdge(1, 0.3), 2, 4);
		clever.addHomology(new HomologyEdge(2, 0.6), 4, 5);
		File expectedFile = new File(RESOURCE_DIR + "hom_1.graphml.xml");
		boolean isSimilar = TestUtils.compareHomologyGraph(clever.getHomology(), expectedFile);
		assertTrue("XML output for homology graph differs from expected", isSimilar);
	}

	@Test
	public void testWriteInterationGraph() throws IOException, SAXException {
		CleverGraph clever = new CleverGraph();
		clever.addVertex(0);
		clever.addVertex(2);
		clever.addVertex(3);
		clever.addVertex(4);
		clever.addVertex(5);
		clever.addInteraction(new InteractionEdge(0, 0.1), 0, 5);
		clever.addInteraction(new InteractionEdge(1, 0.3), 2, 4);
		clever.addInteraction(new InteractionEdge(2, 0.6), 4, 5);
		File expectedFile = new File(RESOURCE_DIR + "int_1.graphml.xml");
		boolean isSimilar = TestUtils.compareInteractionGraph(clever.getInteraction(), expectedFile);
		assertTrue("XML output for interaction graph differs from expected", isSimilar);
	}
}
