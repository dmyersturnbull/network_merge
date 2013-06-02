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
import java.util.List;
import java.util.NavigableSet;
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
import org.structnetalign.Edge;
import org.structnetalign.HomologyEdge;
import org.structnetalign.InteractionEdge;
import org.structnetalign.ReportGenerator;

import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer;
import edu.uci.ics.jung.graph.UndirectedGraph;
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
	 * Returns a subgraph contained in the set {@code cc}, assuming that {@code cc} is a connected component.
	 * 
	 * @param clever
	 * @param cc
	 *            If {@code cc} is not a connected component, the method not return the expected solution
	 * @return
	 */
	private static CleverGraph getSubgraphForCc(CleverGraph clever, Set<Integer> cc) {

		CleverGraph subgraph = new CleverGraph();

		// add vertices
		for (int vertex : clever.getVertices()) {
			if (cc.contains(vertex)) subgraph.addVertex(vertex);
		}

		// add interaction edges
		for (InteractionEdge edge : clever.getInteractions()) {
			Pair<Integer> pair = clever.getInteraction().getEndpoints(edge);
			if (cc.contains(pair.getFirst()) && cc.contains(pair.getSecond())) {
				Pair<Integer> newPair = new Pair<Integer>(pair.getFirst(), pair.getSecond());
				InteractionEdge newEdge = new InteractionEdge(edge.getId(), edge.getWeight());
				subgraph.getInteraction().addEdge(newEdge, newPair);
			}
		}

		// add homology edges
		for (HomologyEdge edge : clever.getHomologies()) {
			Pair<Integer> pair = clever.getHomology().getEndpoints(edge);
			if (cc.contains(pair.getFirst()) && cc.contains(pair.getSecond())) {
				Pair<Integer> newPair = new Pair<Integer>(pair.getFirst(), pair.getSecond());
				HomologyEdge newEdge = new HomologyEdge(edge.getId(), edge.getWeight());
				subgraph.getHomology().addEdge(newEdge, newPair);
			}
		}

		return subgraph;

	}

	public ConcurrentBronKerboschMergeManager(int nCores) {
		super();
		this.nCores = nCores;
	}

	@Override
	public List<MergeUpdate> merge(CleverGraph graph) {

		List<MergeUpdate> updates = new ArrayList<>();

		WeakComponentClusterer<Integer, Edge> alg = new WeakComponentClusterer<>();
		UndirectedGraph<Integer, Edge> combined = graph.buildCombinedGraph();
		Set<Set<Integer>> ccs = alg.transform(combined);
		logger.info("Submitting " + ccs.size() + " connected components as jobs");

		if (ReportGenerator.getInstance() != null) {
			ReportGenerator.getInstance().putInMerged("manager", this.getClass().getSimpleName());
			ReportGenerator.getInstance().putInMerged("n_ccs", ccs.size());
		}

		int nNonTrivialDegenSets = 0;

		// submit jobs
		ExecutorService pool = Executors.newFixedThreadPool(nCores);
		CompletionService<List<NavigableSet<Integer>>> completion = new ExecutorCompletionService<>(pool);

		try {

			List<Future<List<NavigableSet<Integer>>>> futures = new ArrayList<>();
			int index = 1;
			for (Set<Integer> cc : ccs) {
				CleverGraph subgraph = getSubgraphForCc(graph, cc);
				BronKerboschMergeJob job = new BronKerboschMergeJob(subgraph, index);
				Future<List<NavigableSet<Integer>>> future = completion.submit(job);
				futures.add(future);
				index++;
			}

			// now respond to completion
			for (Future<List<NavigableSet<Integer>>> future : futures) {
				List<NavigableSet<Integer>> degenerateSets = null;
				try {
					// We should do this in case the job gets interrupted
					// Sometimes the OS or JVM might do this
					// Use the flag instead of future == null because future.get() may actually return null
					while (degenerateSets == null) {
						try {
							degenerateSets = future.get();
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
				nNonTrivialDegenSets += degenerateSets.size();
				List<MergeUpdate> newUpdates = contract(graph, degenerateSets);
				updates.addAll(newUpdates);

			}

			if (ReportGenerator.getInstance() != null) {
				ReportGenerator.getInstance().putInMerged("n_nontrivial_degenerate_sets", nNonTrivialDegenSets);
			}

		} finally {
			pool.shutdownNow();

			int count = Thread.activeCount()-1;
			if (count > 0) {
				logger.warn("There are " + count + " lingering threads");
			}
		}

		return updates;

	}

}
