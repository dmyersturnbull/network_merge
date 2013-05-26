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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.structnetalign.CleverGraph;
import org.structnetalign.Edge;
import org.structnetalign.HomologyEdge;
import org.structnetalign.InteractionEdge;
import org.structnetalign.util.GraphInteractionAdaptor;
import org.structnetalign.util.GraphMLAdaptor;
import org.structnetalign.util.NetworkUtils;
import org.xml.sax.SAXException;

import psidev.psi.mi.xml.model.EntrySet;
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
public class BronKerboschMergeManager implements MergeManager {

	private static final Logger logger = LogManager.getLogger("org.structnetalign");

	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {

		if (args.length != 3) {
			System.err.println("Usage: BronKerboschMergeManager input-file homology-graph-file output-file");
			return;
		}

		File input = new File(args[0]);
		String graphFile = args[1];
		File output = new File(args[2]);

		// build the homology graph
		UndirectedGraph<Integer, HomologyEdge> homology = GraphMLAdaptor.readHomologyGraph(graphFile);

		// build the interaction graph
		EntrySet entrySet = NetworkUtils.readNetwork(input);
		UndirectedGraph<Integer, InteractionEdge> interaction = GraphInteractionAdaptor.toGraph(entrySet);

		// now make the CleverGraph
		CleverGraph graph = new CleverGraph(interaction, homology);

		// merge!
		BronKerboschMergeManager merge = new BronKerboschMergeManager();
		merge.merge(graph);

		// modify and output
		GraphInteractionAdaptor.modifyProbabilites(entrySet, graph.getInteraction());
		NetworkUtils.writeNetwork(entrySet, output);
	}

	private static <E extends Edge> void move(UndirectedGraph<Integer, E> graph, int v, int v0, AtomicInteger removed,
			AtomicInteger added) {

		// move edges
		List<E> edgesToRemove = new ArrayList<>();
		List<E> edgesToAdd = new ArrayList<>();
		List<Pair<Integer>> edgePairsToAdd = new ArrayList<>();
		Iterator<E> edgeIter = graph.getIncidentEdges(v).iterator();
		Iterator<Integer> neighborIter = graph.getNeighbors(v).iterator();
		while (edgeIter.hasNext()) {
			E edge = edgeIter.next();
			int neighbor = neighborIter.next();
			edgesToRemove.add(edge);
			// as long as it's not an edge within this degenerate set
			// make sure to include v0 in this! don't draw an edge from v to v0
			if (neighbor != v0 && !graph.getNeighbors(v0).contains(neighbor)) {
				edgesToAdd.add(edge);
				edgePairsToAdd.add(new Pair<>(v0, neighbor));
			}
		}

		// remove old interactions
		removed.addAndGet(edgesToRemove.size());
		for (E edge : edgesToRemove) {
			graph.removeEdge(edge);
		}

		// add new interactions
		added.addAndGet(edgesToAdd.size());
		Iterator<E> addIter = edgesToAdd.iterator();
		Iterator<Pair<Integer>> edgePairIter = edgePairsToAdd.iterator();
		while (addIter.hasNext()) {
			E edge = addIter.next();
			Pair<Integer> pair = edgePairIter.next();
			graph.addEdge(edge, pair.getFirst(), pair.getSecond());
		}

	}

	/**
	 * Performs edge contraction on the graph {@code graph}.
	 * 
	 * @param graph
	 * @param cliqueGroups
	 *            A map where the key is any value, and the value is a set of vertices to be merged (can include the key
	 *            vertex)
	 */
	protected static void contract(CleverGraph graph, List<NavigableSet<Integer>> cliqueGroups) {

		if (cliqueGroups.size() > 0) {
			logger.info("Performing edge contraction on " + cliqueGroups.size() + " degenerate sets");
		} else {
			logger.info("No degenerate sets were found");
		}

		for (NavigableSet<Integer> group : cliqueGroups) {

			logger.debug("Performing edge contraction on a degenerate set " + group);

			int v0 = group.first(); // the vertex label we'll actually use

			int nVerticesRemoved = group.size() - 1;

			// okay, this isn't exactly what AtomicInteger is for
			AtomicInteger nHomRemoved = new AtomicInteger(0);
			AtomicInteger nIntRemoved = new AtomicInteger(0);
			AtomicInteger nHomAdded = new AtomicInteger(0);
			AtomicInteger nIntAdded = new AtomicInteger(0);

			for (int v : group) {
				if (v != v0) {
					move(graph.getInteraction(), v, v0, nHomRemoved, nIntRemoved);
					move(graph.getHomology(), v, v0, nHomAdded, nIntAdded);
				}
			}

			for (int v : group) {
				if (v != v0) graph.removeVertex(v);
			}

			logger.debug(nVerticesRemoved + " vertices, " + nHomRemoved + " homology edges, and " + nIntRemoved
					+ " interaction edges contracted into vertex Id#" + v0 + " for degenerate set " + group);

		}
	}

	public BronKerboschMergeManager() {
		super();
	}

	@Override
	public void merge(CleverGraph graph) {
		BronKerboschMergeJob job = new BronKerboschMergeJob(graph, 1);
		try {
			List<NavigableSet<Integer>> cliqueSets = job.call();
			contract(graph, cliqueSets);
		} catch (Exception e) {
			throw new RuntimeException("The merging process failed", e);
		}
	}

}
