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
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.structnetalign.CleverGraph;
import org.structnetalign.HomologyEdge;
import org.structnetalign.util.NetworkUtils;

public class BronKerboschMergeJob implements Callable<List<List<Integer>>> {

	private static final Logger logger = LogManager.getLogger("org.structnetalign");
	
	private CleverGraph graph;
	
	private int index;
	
	private static String hashVertexInteractions(Collection<Integer> vertexInteractionNeighbors) {
		return NetworkUtils.hash(vertexInteractionNeighbors);
	}

	public BronKerboschMergeJob(CleverGraph graph, int index) {
		super();
		this.graph = graph;
		this.index = index;
	}

	@Override
	public List<List<Integer>> call() throws Exception {

		logger.info("Searching for cliques on job " + index + " containing " + graph.getVertexCount() + " vertices and " + graph.getHomologyCount() + " homology edges");
		
		// find the cliques
		BronKerboschCliqueFinder<Integer, HomologyEdge> finder = new BronKerboschCliqueFinder<>();
		Collection<Set<Integer>> cliques = finder.transform(graph.getHomology());

		logger.info("Found " + cliques.size() + " cliques on job " + index);
		
		// group the cliques by sets of interactions
		NavigableMap<String, Collection<Integer>> cliqueGroups = new TreeMap<>();
		for (Set<Integer> clique : cliques) {
			for (int v : clique) {
				Collection<Integer> neighbors = graph.getInteractionNeighbors(v);
				String hash = hashVertexInteractions(neighbors);
				Collection<Integer> group = cliqueGroups.get(hash);
				if (group == null) {
					group = new TreeSet<>();
					cliqueGroups.put(hash, group);
				}
				group.add(v);
				logger.trace("Found " + hash + " --> " + cliqueGroups.get(hash) + " (job " + index + ")");
			}
		}

		logger.info("Found " + cliqueGroups.size() + " degenerate sets found on job " + index);
		
		// now we just want a set to return
		List<List<Integer>> list = new ArrayList<>(cliqueGroups.size());
		for (Collection<Integer> c : cliqueGroups.values()) {
			List<Integer> a = new ArrayList<Integer>(c.size());
			a.addAll(c);
			list.add(a);
			logger.debug("Found degenerate set " + a + " (job " + index + ")");
		}
//		list.addAll(cliqueGroups.values());

		return list;
	}

}
