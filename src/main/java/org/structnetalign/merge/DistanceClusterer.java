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

import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections15.Transformer;
import org.structnetalign.util.EdgeWeighter;

import edu.uci.ics.jung.graph.Graph;

/**
 * A {@link Transformer} determines the set of vertices that easily reachable from a root vertex. A vertex v is easily
 * reachable from a root vertex r if:
 * <ol>
 * <li>there is a path from r to v, and</li>
 * <li>The path from v to r traversing the fewest vertices has a score above, below, or between some threshold(s)</li>
 * </ol>
 * The scoring of a path is handled by {@link #visit(Object, Object)}, {@link #unvisit(Object, Object)},
 * {@link #isWithinRange(Object, Object)}, and the callback interface {@link EdgeWeighter}.
 * 
 * @author dmyersturnbull
 * 
 * @param <V> The type of the vertices
 * @param <E> The type of the edges
 */
public abstract class DistanceClusterer<V, E> {

	private final EdgeWeighter<E> edgeWeighter;
	
	public DistanceClusterer(EdgeWeighter<E> edgeWeighter) {
		this.edgeWeighter = edgeWeighter;
	}

	/**
	 *
	 * @param graph
	 * @param roots
	 * @return For each root, the set of other roots that are easily reachable, including the parent root
	 */
	public final Map<V,Set<V>> transform(Graph<V, E> graph, Collection<V> roots) {

		Map<V,Set<V>> reachableMap = new HashMap<V,Set<V>>();

		for (V root : roots) {

			Set<V> reachable = new HashSet<>();
			HashSet<V> unvisited = new HashSet<V>(graph.getVertices());

			// a map from every vertex to the edge used to get to it
			// works because only visit a vertex once
			HashMap<V, E> edgesTaken = new HashMap<V, E>();

			Deque<V> queue = new LinkedList<V>();
			queue.add(root);
			reachable.add(root);

			while (!queue.isEmpty()) {

				V vertex = queue.remove();
				E edge = edgesTaken.get(vertex);
				unvisited.remove(vertex);
				
				// stop traversing if we're too far
				if (!isWithinRange(root, vertex)) continue;
				
				reachable.add(vertex); // not that this is AFTER the within-range check
				
				if (edge != null) visit(vertex, edge); // this is the ONLY place where we "officially" VISIT a vertex

				Collection<V> neighbors = graph.getNeighbors(vertex);
				for (V neighbor : neighbors) {
					if (unvisited.contains(neighbor)) {
						queue.add(neighbor);
						E edgeToNeighbor = graph.findEdge(vertex, neighbor);
						edgesTaken.put(neighbor, edgeToNeighbor);
					}
				}

				unvisit(vertex, edge); // this is the ONLY place where we "officially" UNVISIT a vertex

			}
			
			reachableMap.put(root, reachable);

		}

		return reachableMap;
	}

	protected final double getEdgeWeight(E edge) {
		return edgeWeighter.getWeight(edge);
	}

	protected abstract boolean isWithinRange(V root, V vertex);

	protected abstract void unvisit(V vertex, E edge);

	protected abstract void visit(V vertex, E edge);

}
