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

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Pair;

public class GraphMerger {
	
	@SafeVarargs
	public static <V,E> Graph<V,E> merge(Graph<V,E>... graphs) {
		Graph<V,E> myGraph = graphs[0];
		for (int i = 1; i < graphs.length; i++) {
			Graph<V,E> graph = graphs[i];
			for (V vertex : graph.getVertices()) {
				myGraph.addVertex(vertex);
			}
			for (E edge : graph.getEdges()) {
				Pair<V> vertices = graph.getEndpoints(edge);
				myGraph.addEdge(edge, vertices);
			}
		}
		return myGraph;
	}
	
}
