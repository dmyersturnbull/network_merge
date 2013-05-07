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
 * @author dmyersturnbull
 */
package org.structnetalign.merge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.graph.UndirectedGraph;

public class BronKerboschCliqueFinder<V,E> implements Transformer<UndirectedGraph<V,E>, Collection<Set<V>>>  {

	@Override
	public Collection<Set<V>> transform(UndirectedGraph<V, E> graph) {
		Collection<Set<V>> cliques = new TreeSet<>();
		// TODO another implementation
		InternalBronKerboschCliqueFinder<V,E> finder = new InternalBronKerboschCliqueFinder<V,E>(graph);
		cliques = finder.getAllMaximalCliques();
		return cliques;
	}


	/**
	 * 
	 * This code was written by <a href="https://plus.google.com/116035002500528609208/">Reuben Doetsch</a>,
	 * who adapted it from <a href="http://jgrapht.org/">JGraphT</a>. Since JGraphT is under the LGPL, it is unclear which
	 * license is under effect here. At the least, the code is OSG-certified Free and is not my work.
	 * 
	 * Adopted from JGraphT for use with Jung. The Bron-Kerbosch algorithm is an algorithm for finding maximal cliques in an
	 * undirected graph This algorithmn is taken from Coenraad Bron- Joep Kerbosch in 1973. This works on undirected graph
	 * See {@linktourl See http://en.wikipedia.org/wiki/Bron%E2%80%93Kerbosch_algorithm}
	 * 
	 * @author Reuben Doetsch
	 * 
	 * @param <V>
	 *            vertex class of graph
	 * @param <E>
	 *            edge class of graph
	 */
	private static class InternalBronKerboschCliqueFinder<V, E> {

		private Collection<Set<V>> cliques;

		private final UndirectedGraph<V, E> graph;

		/**
		 * Creates a new clique finder. Make sure this is a simple graph.
		 * 
		 * @param graph
		 *            the graph in which cliques are to be found; graph must be simple
		 */
		public InternalBronKerboschCliqueFinder(UndirectedGraph<V, E> graph) {

			this.graph = graph;
		}

		/**
		 * Finds all maximal cliques of the graph. A clique is maximal if it is impossible to enlarge it by adding another
		 * vertex from the graph. Note that a maximal clique is not necessarily the biggest clique in the graph.
		 * 
		 * @return Collection of cliques (each of which is represented as a Set of vertices)
		 */
		public Collection<Set<V>> getAllMaximalCliques() {
			// TODO: assert that graph is simple

			cliques = new ArrayList<Set<V>>();
			List<V> potential_clique = new ArrayList<V>();
			List<V> candidates = new ArrayList<V>();
			List<V> already_found = new ArrayList<V>();
			candidates.addAll(graph.getVertices());
			findCliques(potential_clique, candidates, already_found);
			return cliques;
		}

		/**
		 * Finds the biggest maximal cliques of the graph.
		 * 
		 * @return Collection of cliques (each of which is represented as a Set of vertices)
		 */
		public Collection<Set<V>> getBiggestMaximalCliques() {
			// first, find all cliques
			getAllMaximalCliques();

			int maximum = 0;
			Collection<Set<V>> biggest_cliques = new ArrayList<Set<V>>();
			for (Set<V> clique : cliques) {
				if (maximum < clique.size()) {
					maximum = clique.size();
				}
			}
			for (Set<V> clique : cliques) {
				if (maximum == clique.size()) {
					biggest_cliques.add(clique);
				}
			}
			return biggest_cliques;
		}

		private boolean end(List<V> candidates, List<V> already_found) {
			// if a node in already_found is connected to all nodes in candidates
			boolean end = false;
			int edgecounter;
			for (V found : already_found) {
				edgecounter = 0;
				for (V candidate : candidates) {
					if (graph.isNeighbor(found, candidate)) {
						edgecounter++;
					} // of if
				} // of for
				if (edgecounter == candidates.size()) {
					end = true;
				}
			} // of for
			return end;
		}

		private void findCliques(List<V> potential_clique, List<V> candidates, List<V> already_found) {
			List<V> candidates_array = new ArrayList<V>(candidates);
			if (!end(candidates, already_found)) {
				// for each candidate_node in candidates do
				for (V candidate : candidates_array) {
					List<V> new_candidates = new ArrayList<V>();
					List<V> new_already_found = new ArrayList<V>();

					// move candidate node to potential_clique
					potential_clique.add(candidate);
					candidates.remove(candidate);

					// create new_candidates by removing nodes in candidates not
					// connected to candidate node
					for (V new_candidate : candidates) {
						if (graph.isNeighbor(candidate, new_candidate)) {
							new_candidates.add(new_candidate);
						} // of if
					} // of for

					// create new_already_found by removing nodes in already_found
					// not connected to candidate node
					for (V new_found : already_found) {
						if (graph.isNeighbor(candidate, new_found)) {
							new_already_found.add(new_found);
						} // of if
					} // of for

					// if new_candidates and new_already_found are empty
					if (new_candidates.isEmpty() && new_already_found.isEmpty()) {
						// potential_clique is maximal_clique
						cliques.add(new HashSet<V>(potential_clique));
					} // of if
					else {
						// recursive call
						findCliques(potential_clique, new_candidates, new_already_found);
					} // of else

					// move candidate_node from potential_clique to already_found;
					already_found.add(candidate);
					potential_clique.remove(candidate);
				} // of for
			} // of if
		}
	}

}
