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

import java.text.DecimalFormat;
import java.text.NumberFormat;
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.structnetalign.CleverGraph;
import org.structnetalign.Edge;
import org.structnetalign.HomologyEdge;
import org.structnetalign.InteractionEdge;
import org.structnetalign.util.EdgeWeighter;

import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.graph.util.Pair;

public class HomologySearchJob implements Callable<InteractionEdgeUpdate> {

	private static final Logger logger = LogManager.getLogger("org.structnetalign");

	private CleverGraph graph;
	private int maxDepth = Integer.MAX_VALUE;
	private final InteractionEdge root;
	private final int rootA;
	private final int rootB;

	public HomologySearchJob(InteractionEdge root, CleverGraph graph) {
		super();
		this.graph = graph;
		this.root = root;
		Pair<Integer> pair = graph.getInteraction().getEndpoints(root);
		rootA = pair.getFirst();
		rootB = pair.getSecond();
	}

	@Override
	public InteractionEdgeUpdate call() throws Exception {
		double score = 0;
		int nUpdates = 0;
		InteractionEdgeUpdate update = new InteractionEdgeUpdate(root, rootA, rootB);
		Map<Integer, Double> distancesToA = findDistances(rootA, graph.getHomology());
		Map<Integer, Double> distancesToB = findDistances(rootB, graph.getHomology());
		for (Map.Entry<Integer, Double> a : distancesToA.entrySet()) {
			for (Map.Entry<Integer, Double> b : distancesToB.entrySet()) {
				InteractionEdge interaction = graph.getInteraction().findEdge(a.getKey(), b.getKey());
				if (interaction != null && interaction.getId() != root.getId()) {
					Double distToA = a.getValue();
					Double distToB = b.getValue();
					if (distToA == null || distToB == null) continue; // we can't reach one of a or b
//					double updateProb = interaction.getWeight() * Math.exp(distToA) * Math.exp(distToB);
//					score += updateProb - score * updateProb;
					double scoreA = 1 - Math.exp(distToA);
					double scoreB = 1 - Math.exp(distToB);
					double aScore = interaction.getWeight() * (1 - scoreA - scoreB + scoreA*scoreB);
					logger.debug("Updating " + root + " (" + rootA + ", " + rootB + ")" + " with score " + nf.format(aScore) + " due to " + interaction + " (" + a.getKey() + ", " + b.getKey() + ")");
					score += aScore - score * aScore;
					nUpdates++;
				}
			}
		}
		update.setScore(score);
		update.setnUpdates(nUpdates);
		return update;
	}

	public void setMaxDepth(int maxDepth) {
		this.maxDepth = maxDepth;
	}

	private static NumberFormat nf = new DecimalFormat();
	static {
		nf.setMinimumFractionDigits(1);
		nf.setMaximumFractionDigits(3);
	}
	
	private <V, E extends Edge> NavigableMap<V, Double> findDistances(V root, UndirectedGraph<V, E> graph) {

		NavigableMap<V, Double> map = new TreeMap<>();
		map.put(root, 1.0);

		// need constant-time get
		HashSet<V> unvisited = new HashSet<V>(graph.getVertices());

		HashMap<V, V> verticesTaken = new HashMap<>();
		HashMap<V, E> edgesTaken = new HashMap<>();
		HashMap<V, Integer> distances = new HashMap<>();
		distances.put(root, 0);


		Deque<V> queue = new LinkedList<V>();
		queue.add(root);

		while (!queue.isEmpty()) {

			V vertex = queue.remove();
			E edge = edgesTaken.get(vertex);
			unvisited.remove(vertex);

//			 stop traversing if we're too far
			int distance = distances.get(vertex);
			if (distance > maxDepth) {
				logger.trace("Distance of " + distance + " exceeded");
				continue;
			}
			
			double totalWeight = Math.log(1);
			V parent = verticesTaken.get(vertex);
			if (parent != null) { // we're not on the root
				double weight = Math.log(edge.getWeight());
				totalWeight = map.get(parent) + weight;
			}
			logger.trace("Weight for " + vertex + " is " + nf.format(totalWeight));
			map.put(vertex, totalWeight);
			
			Collection<V> neighbors = graph.getNeighbors(vertex);
			for (V neighbor : neighbors) {
				if (unvisited.contains(neighbor)) {
					queue.add(neighbor);
					E edgeToNeighbor = graph.findEdge(vertex, neighbor);
					distances.put(neighbor, distance + 1);
					verticesTaken.put(neighbor, vertex);
					edgesTaken.put(neighbor, edgeToNeighbor);
				}
			}

		}

		for (Map.Entry<V, Double> entry : map.entrySet()) {
			logger.trace("Prob(" + root + ", " + entry.getKey() + ") = " + nf.format(Math.exp(entry.getValue())));
		}
		return map;

	}

}
