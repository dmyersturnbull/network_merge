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

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import org.structnetalign.CleverGraph;
import org.structnetalign.HomologyEdge;
import org.structnetalign.InteractionEdge;
import org.structnetalign.util.GraphMLAdaptor;

import edu.uci.ics.jung.graph.UndirectedGraph;

public class WeightManagerTest {

	private static final String RESOURCE_DIR = "src/test/resources/weight/";
	
	public static UndirectedGraph<Integer, HomologyEdge> testSimple(WeightManager manager) {
		File interactionInput = new File(RESOURCE_DIR + "simple_interaction.graphml.xml");
		UndirectedGraph<Integer,InteractionEdge> interaction = GraphMLAdaptor.readInteractionGraph(interactionInput);
		CleverGraph graph = new CleverGraph(interaction);
		Map<Integer,String> uniProtIds = new TreeMap<Integer,String>();
		uniProtIds.put(1, "P29392");
		uniProtIds.put(2, "P35495");
		uniProtIds.put(3, "Q9S0X0");
		uniProtIds.put(4, "P10410");
		uniProtIds.put(5, "Q9RCK8");
		uniProtIds.put(6, "Q56268");
		manager.assignWeights(graph, uniProtIds);
		return graph.getHomology();
	}
	
}
