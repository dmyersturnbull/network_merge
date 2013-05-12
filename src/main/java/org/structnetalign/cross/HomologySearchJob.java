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

import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import org.structnetalign.CleverGraph;
import org.structnetalign.HomologyEdge;
import org.structnetalign.InteractionEdge;
import org.structnetalign.util.EdgeWeighter;

import edu.uci.ics.jung.graph.UndirectedGraph;

public class HomologySearchJob implements Callable<InteractionUpdate> {

	private CleverGraph graph;
	private int maxDepth = Integer.MAX_VALUE;
	private final InteractionEdge root;
	private final int rootA;
	private final int rootB;

	private EdgeWeighter<HomologyEdge> weighter = new EdgeWeighter<HomologyEdge>() {
		@Override
		public double getWeight(HomologyEdge e) {
			return e.getScore();
		}
	};

	public HomologySearchJob(InteractionEdge root, CleverGraph graph) {
		super();
		this.graph = graph;
		this.root = root;
		rootA = graph.getInteraction().getSource(root);
		rootB = graph.getInteraction().getDest(root);
	}

	@Override
	public InteractionUpdate call() throws Exception {
		double score = 0;
		InteractionUpdate update = new InteractionUpdate(root, rootA, rootB);
		Map<Integer, Double> distancesToA = findDistances(rootA, graph.getHomology(), weighter);
		Map<Integer, Double> distancesToB = findDistances(rootB, graph.getHomology(), weighter);
		for (Map.Entry<Integer, Double> a : distancesToA.entrySet()) {
			for (Map.Entry<Integer, Double> b : distancesToB.entrySet()) {
				InteractionEdge interaction = graph.getInteraction().findEdge(a.getKey(), b.getKey());
				if (interaction != null) {
					double scoreA = 1 - Math.exp(a.getValue());
					double scoreB = 1 - Math.exp(b.getValue());
					score += interaction.getProbability() * (1 - scoreA - scoreB + scoreA*scoreB);
				}
			}
		}
		update.setScore(score);
		return update;
	}

	public void setMaxDepth(int maxDepth) {
		this.maxDepth = maxDepth;
	}

	private <V, E> NavigableMap<V, Double> findDistances(V root, UndirectedGraph<V, E> graph, EdgeWeighter<E> weighter) {

		NavigableMap<V, Double> map = new TreeMap<>();

		// need constant-time get
		HashSet<V> unvisited = new HashSet<V>(graph.getVertices());

		HashMap<V, E> edgesTaken = new HashMap<>();
		HashMap<V, Integer> distances = new HashMap<>();

		// first get a root for the cluster
		Set<V> cluster = new HashSet<V>();
		unvisited.remove(root);
		cluster.add(root);

		Deque<V> queue = new LinkedList<V>();
		queue.add(root);

		while (!queue.isEmpty()) {

			V vertex = queue.remove();
			E edge = edgesTaken.get(vertex);

			// stop traversing if we're too far
			int distance = distances.get(vertex);
			if (distance > maxDepth) continue;

			Collection<V> neighbors = graph.getNeighbors(vertex);
			for (V neighbor : neighbors) {
				if (unvisited.contains(neighbor)) {
					queue.add(neighbor);
					unvisited.remove(neighbor);
					cluster.add(neighbor);
					E edgeToNeighbor = graph.findEdge(vertex, neighbor);
					distances.put(neighbor, distance + 1);
					double score = Math.log(weighter.getWeight(edge));
					map.put(neighbor, map.get(vertex) + score);
					edgesTaken.put(neighbor, edgeToNeighbor);
				}
			}

		}

		return map;

	}

}
