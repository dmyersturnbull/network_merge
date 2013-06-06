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
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.structnetalign.CleverGraph;
import org.structnetalign.Edge;
import org.structnetalign.util.GraphMLAdaptor;
import org.xml.sax.SAXException;

import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.graph.util.Pair;

/**
 * A {@link MergeManager} that finds degenerate sets, using a modification of the Bron–Kerbosch algorithm for
 * Max-Clique.
 * 
 * @author dmyersturnbull
 * @see BronKerboschCliqueFinder
 */
public class BronKerboschMergeManager implements MergeManager {

	private static final Logger logger = LogManager.getLogger("org.structnetalign");

	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {

		if (args.length != 4) {
			System.err
					.println("Usage: "
							+ BronKerboschMergeManager.class.getSimpleName()
							+ " interaction-graph-file homology-graph-file output-interaction-graph-file output-homology-graph-file");
			return;
		}

		File interactionFile = new File(args[0]);
		File homologyFile = new File(args[1]);
		File outputInteraction = new File(args[2]);
		File outputHomology = new File(args[3]);

		CleverGraph graph = GraphMLAdaptor.readGraph(interactionFile, homologyFile);

		BronKerboschMergeManager merge = new BronKerboschMergeManager();
		merge.merge(graph);

		GraphMLAdaptor.writeInteractionGraph(graph.getInteraction(), outputInteraction);
		GraphMLAdaptor.writeHomologyGraph(graph.getHomology(), outputHomology);
	}

	private static <E extends Edge> void move(UndirectedGraph<Integer, E> graph, int v, int v0, AtomicInteger removed,
			AtomicInteger added, Set<E> edgesRemovedOrMoved) {

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
				edgesRemovedOrMoved.add(edge);
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
	protected static List<MergeUpdate> contract(CleverGraph graph, List<NavigableSet<Integer>> cliqueGroups) {

		if (cliqueGroups.size() > 0) {
			logger.info("Performing edge contraction on " + cliqueGroups.size() + " degenerate sets");
		} else {
			logger.info("No degenerate sets were found");
		}

		List<MergeUpdate> updates = new ArrayList<>(cliqueGroups.size());

		for (NavigableSet<Integer> group : cliqueGroups) {

			logger.debug("Performing edge contraction on a degenerate set " + group);

			int v0 = group.first(); // the vertex label we'll actually use

			MergeUpdate update = new MergeUpdate(v0);
			updates.add(update);

			int nVerticesRemoved = group.size() - 1;

			// okay, this isn't exactly what AtomicInteger is for
			AtomicInteger nHomRemoved = new AtomicInteger(0);
			AtomicInteger nIntRemoved = new AtomicInteger(0);
			AtomicInteger nHomAdded = new AtomicInteger(0);
			AtomicInteger nIntAdded = new AtomicInteger(0);

			for (int v : group) {
				update.addVertex(v);
				if (v != v0) {
					move(graph.getInteraction(), v, v0, nHomRemoved, nIntRemoved, update.getInteractionEdges());
					move(graph.getHomology(), v, v0, nHomAdded, nIntAdded, update.getHomologyEdges());
				}
			}

			for (int v : group) {
				if (v != v0) graph.removeVertex(v);
			}

			logger.debug(nVerticesRemoved + " vertices, " + nHomRemoved + " homology edges, and " + nIntRemoved
					+ " interaction edges contracted into vertex Id#" + v0 + " for degenerate set " + group);

		}

		return updates;

	}

	public BronKerboschMergeManager() {
		super();
	}

	@Override
	public List<MergeUpdate> merge(CleverGraph graph) {
		BronKerboschMergeJob job = new BronKerboschMergeJob(graph, 1);
		try {
			List<NavigableSet<Integer>> cliqueSets = job.call();
			return contract(graph, cliqueSets);
		} catch (Exception e) {
			throw new RuntimeException("The merging process failed", e);
		}
	}

}
