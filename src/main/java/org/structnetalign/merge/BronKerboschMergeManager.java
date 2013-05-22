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
import java.util.Collection;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;

import org.structnetalign.CleverGraph;
import org.structnetalign.HomologyEdge;
import org.structnetalign.InteractionEdge;
import org.structnetalign.util.GraphInteractionAdaptor;
import org.structnetalign.util.GraphMLAdaptor;
import org.structnetalign.util.NetworkUtils;
import org.xml.sax.SAXException;

import psidev.psi.mi.xml.model.EntrySet;
import edu.uci.ics.jung.graph.UndirectedGraph;

/**
 * A {@link MergeManager} that finds cliques whose members share interactions, using a
 * {@link ProbabilisticDistanceCluster} and a modification of the Bron–Kerbosch algorithm for Max-Clique. The runtime is
 * approximately
 * 
 * @author dmyersturnbull
 * @see BronKerboschCliqueFinder
 */
public class BronKerboschMergeManager implements MergeManager {

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
		UndirectedGraph<Integer, InteractionEdge> interaction = GraphInteractionAdaptor.toGraph(entrySet, 0.5);

		// now make the CleverGraph
		CleverGraph graph = new CleverGraph(interaction, homology);

		// merge!
		BronKerboschMergeManager merge = new BronKerboschMergeManager();
		merge.merge(graph);

		// modify and output
		GraphInteractionAdaptor.modifyProbabilites(entrySet, graph.getInteraction());
		NetworkUtils.writeNetwork(entrySet, output);
	}

	/**
	 * Performs edge contraction on the graph {@code graph}.
	 * 
	 * @param graph
	 * @param cliqueGroups
	 *            A map where the key is any value, and the value is a set of vertices to be merged (can include the key
	 *            vertex)
	 */
	protected static void contract(CleverGraph graph, Collection<Collection<Integer>> cliqueGroups) {
		// now perform edge contraction
		for (Collection<Integer> group : cliqueGroups) {

			int v0 = -1; // the vertex label we'll actually use
			int i = 0;

			for (int v : group) {

				if (i != 0) {

					// move interactions
					Iterator<InteractionEdge> interactionIter = graph.getInteractions(v).iterator();
					Iterator<Integer> neighborIter = graph.getInteractionNeighbors(v).iterator();
					while (interactionIter.hasNext()) {
						InteractionEdge interaction = interactionIter.next();
						int neighbor = neighborIter.next();
						if (!graph.getInteractionNeighbors(v0).contains(neighbor)) {
							graph.addInteraction(interaction, v0, neighbor);
						}
						graph.removeInteraction(interaction);
					}

					// just remove homology edges
					for (HomologyEdge edge : graph.getHomologies(v)) {
						graph.removeHomology(edge);
					}

					// finally the vertex has no remaining edges; remove it
					graph.removeVertex(v);

				} else {
					v0 = v;
				}
				i++;
			}
		}
	}

	public BronKerboschMergeManager() {
		super();
	}

	@Override
	public void merge(CleverGraph graph) {
		BronKerboschMergeJob job = new BronKerboschMergeJob(graph);
		try {
			Collection<Collection<Integer>> cliqueSets = job.call();
			contract(graph, cliqueSets);
		} catch (Exception e) {
			throw new RuntimeException("The merging process failed", e);
		}
	}

}
