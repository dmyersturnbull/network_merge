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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.structnetalign.CleverGraph;
import org.structnetalign.HomologyEdge;
import org.structnetalign.InteractionEdge;

import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer;
import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.util.Pair;

/**
 * A {@link MergeManager} that finds cliques whose members share interactions, using a
 * {@link ProbabilisticDistanceCluster} and a modification of the Bron–Kerbosch algorithm for Max-Clique. The runtime is
 * approximately
 * 
 * @author dmyersturnbull
 * @see BronKerboschCliqueFinder
 */
public class ConcurrentBronKerboschMergeManager extends BronKerboschMergeManager {

	private static final Logger logger = LogManager.getLogger("org.structnetalign");

	private int nCores;

	/**
	 * Returns a set of connected components in the {@link CleverGraph}. No distinct connected components may share
	 * homology edges <em>or</em> interaction edges.
	 * 
	 * @param clever
	 * @return
	 */
	private static Set<Set<Integer>> cleverCcs(CleverGraph clever) {

		// sometimes the stupidest solutions are the best
		// we're just going to make a new graph containing both types of edges
		UndirectedGraph<Integer, Integer> combined = new UndirectedSparseGraph<>();
		for (int vertex : clever.getVertices()) {
			combined.addVertex(vertex);
		}
		int i = 0;
		for (HomologyEdge edge : clever.getHomologies()) {
			Pair<Integer> vertices = clever.getHomology().getEndpoints(edge);
			combined.addEdge(i, vertices);
			i++;
		}
		// do NOT reset i
		for (InteractionEdge edge : clever.getInteractions()) {
			Pair<Integer> vertices = clever.getInteraction().getEndpoints(edge);
			combined.addEdge(i, vertices);
			i++;
		}

		WeakComponentClusterer<Integer, Integer> alg = new WeakComponentClusterer<>();
		Set<Set<Integer>> ccs = alg.transform(combined);
		return ccs;
	}

	/**
	 * Returns a subgraph contained in the set {@code cc}, assuming that {@code cc} is a connected component.
	 * 
	 * @param whole
	 * @param cc
	 *            If {@code cc} is not a connected component, the method not return the expected solution
	 * @return
	 */
	private static CleverGraph getSubgraphForCc(CleverGraph whole, Set<Integer> cc) {

		// first let's map each edge (of both types) to either of its vertices
		// we can do this because we have a cc
		// do it this way to get from nlogn to n
		HashMap<Integer, Integer> homToVertex = new HashMap<>();
		int i = 0;
		for (HomologyEdge edge : whole.getHomologies()) {
			homToVertex.put(i, whole.getHomology().getSource(edge)); // again, either is fine
			i++;
		}
		HashMap<Integer, Integer> intToVertex = new HashMap<>();
		i = 0; // DO reset i
		for (InteractionEdge edge : whole.getInteractions()) {
			homToVertex.put(i, whole.getInteraction().getSource(edge));
			i++;
		}

		// add vertices
		CleverGraph subgraph = new CleverGraph();
		for (int vertex : whole.getVertices()) {
			if (cc.contains(vertex)) subgraph.addVertex(vertex);
		}

		// add interaction edges
		i = 0;
		for (InteractionEdge edge : whole.getInteractions()) {
			int aVertex = intToVertex.get(i);
			if (cc.contains(aVertex)) {
				subgraph.addInteraction(edge, whole.getInteraction().getSource(edge),
						whole.getInteraction().getDest(edge));
			}
			i++;
		}

		// add homology edges
		i = 0;
		for (HomologyEdge edge : whole.getHomologies()) {
			int aVertex = intToVertex.get(i);
			if (cc.contains(aVertex)) {
				subgraph.addHomology(edge, whole.getHomology().getSource(edge), whole.getHomology().getDest(edge));
			}
			i++;
		}
		return subgraph;
	}

	public ConcurrentBronKerboschMergeManager(int nCores, double xi) {
		super();
		this.nCores = nCores;
	}

	@Override
	public void merge(CleverGraph graph) {

		Set<Set<Integer>> ccs = cleverCcs(graph);

		// submit jobs
		ExecutorService pool = Executors.newFixedThreadPool(nCores);
		CompletionService<List<List<Integer>>> completion = new ExecutorCompletionService<>(pool);
		List<Future<List<List<Integer>>>> futures = new ArrayList<>();
		int index = 1;
		for (Set<Integer> cc : ccs) {
			CleverGraph subgraph = getSubgraphForCc(graph, cc);
			BronKerboschMergeJob job = new BronKerboschMergeJob(subgraph, index);
			Future<List<List<Integer>>> future = completion.submit(job);
			futures.add(future);
			index++;
		}

		// now respond to completion
		for (Future<List<List<Integer>>> future : futures) {
			List<List<Integer>> cliqueGroups = null;
			try {
				// We should do this in case the job gets interrupted
				// Sometimes the OS or JVM might do this
				// Use the flag instead of future == null because future.get() may actually return null
				while (cliqueGroups == null) {
					try {
						cliqueGroups = future.get();
					} catch (InterruptedException e1) {
						logger.warn(
								"A thread was interrupted while waiting for a connected component merging process. Retrying.",
								e1);
					}
				}
			} catch (ExecutionException e) {
				logger.error("Encountered an error running the merging process for a connected component. Skipping", e);
			}

			// now add the result to the clevergraph
			contract(graph, cliqueGroups);

		}

	}

}
