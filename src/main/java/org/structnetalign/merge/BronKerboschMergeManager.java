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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.codec.binary.Hex;
import org.structnetalign.CleverGraph;
import org.structnetalign.HomologyEdge;
import org.structnetalign.InteractionEdge;
import org.structnetalign.util.GraphInteractionAdaptor;
import org.structnetalign.util.NetworkUtils;
import org.xml.sax.SAXException;

import psidev.psi.mi.xml.model.EntrySet;
import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer;
import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.io.GraphMLReader;

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
		GraphMLReader<UndirectedGraph<Integer,HomologyEdge>,Integer,HomologyEdge> reader = new GraphMLReader<>();
		UndirectedGraph<Integer,HomologyEdge> homology = new UndirectedSparseGraph<>();
		reader.load(graphFile, homology);
		
		// build the interaction graph
		EntrySet entrySet = NetworkUtils.readNetwork(input);
		UndirectedGraph<Integer,InteractionEdge> interaction = GraphInteractionAdaptor.toGraph(entrySet, 0.5);
		
		// now make the CleverGraph
		CleverGraph graph = new CleverGraph(interaction, homology);
		
		// merge!
		BronKerboschMergeManager merge = new BronKerboschMergeManager();
		merge.merge(graph);
		
		// modify and output
		GraphInteractionAdaptor.modifyProbabilites(entrySet, graph.getInteraction());
		NetworkUtils.writeNetwork(entrySet, output);
	}

	@Override
	public void merge(CleverGraph graph) {

		// define the equivalence relation we need
		WeakComponentClusterer<Integer, HomologyEdge> alg = new WeakComponentClusterer<>();
		Set<Set<Integer>> ccs = alg.transform(graph.getHomology());
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();
		for (Set<Integer> cc : ccs) {
			int v0 = -1;
			int i = 0;
			for (int v : cc) {
				if (i == 0) v0 = v;
				map.put(v, v0);
				i++;
			}
		}

		// find the cliques
		BronKerboschCliqueFinder<Integer, HomologyEdge> finder = new BronKerboschCliqueFinder<>();
		Collection<Set<Integer>> cliques = finder.transform(graph.getHomology());

		// group the cliques by sets of interactions
		NavigableMap<String, Collection<Integer>> cliqueGroups = new TreeMap<>();
		for (Set<Integer> clique : cliques) {
			for (int v : clique) {
				Collection<Integer> neighbors = graph.getInteractionNeighbors(v);
				String hash = hashVertexInteractions(neighbors, map);
				Collection<Integer> group = cliqueGroups.get(hash);
				if (group == null) group = new TreeSet<>();
				group.add(v);
			}
		}

		// now perform edge contraction
		for (Collection<Integer> group : cliqueGroups.values()) {
			
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

	private String hashVertexInteractions(Collection<Integer> vertexInteractionNeighbors, Map<Integer, Integer> map) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Couldn't find the algorithm MD5", e);
		}
		StringBuilder sb = new StringBuilder();
		for (int neighbor : vertexInteractionNeighbors) {
			sb.append(map.get(neighbor)); // use the equivalence relation here
		}
		byte[] bytes = md.digest(sb.toString().getBytes());
		return new String(Hex.encodeHex(bytes));
	}

}
