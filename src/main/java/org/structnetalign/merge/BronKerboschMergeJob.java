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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import org.apache.commons.codec.binary.Hex;
import org.structnetalign.CleverGraph;
import org.structnetalign.HomologyEdge;

public class BronKerboschMergeJob implements Callable<Collection<Collection<Integer>>> {

	private CleverGraph graph;
	
	private static String hashVertexInteractions(Collection<Integer> vertexInteractionNeighbors) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Couldn't find the algorithm MD5", e);
		}
		StringBuilder sb = new StringBuilder();
		for (int neighbor : vertexInteractionNeighbors) {
			sb.append(neighbor + ",");
		}
		byte[] bytes = md.digest(sb.toString().getBytes());
		return new String(Hex.encodeHex(bytes));
	}

	public BronKerboschMergeJob(CleverGraph graph) {
		super();
		this.graph = graph;
	}

	@Override
	public Collection<Collection<Integer>> call() throws Exception {

		// find the cliques
		BronKerboschCliqueFinder<Integer, HomologyEdge> finder = new BronKerboschCliqueFinder<>();
		Collection<Set<Integer>> cliques = finder.transform(graph.getHomology());

		// group the cliques by sets of interactions
		NavigableMap<String, Collection<Integer>> cliqueGroups = new TreeMap<>();
		for (Set<Integer> clique : cliques) {
			for (int v : clique) {
				Collection<Integer> neighbors = graph.getInteractionNeighbors(v);
				String hash = hashVertexInteractions(neighbors);
				Collection<Integer> group = cliqueGroups.get(hash);
				if (group == null) group = new TreeSet<>();
				group.add(v);
			}
		}

		// now we just want a set to return
		Set<Collection<Integer>> set = new TreeSet<>();
		set.addAll(cliqueGroups.values());

		return set;
	}

}
