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
import java.util.Set;

import org.apache.commons.collections15.Transformer;
import org.structnetalign.util.EdgeWeighter;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedGraph;

/**
 * A {@link Transformer} that clusters vertices by their reachability. Two vertices u and v are clustered together if
 * and only if:
 * <ol>
 * <li>u is reachable from v, and</li>
 * <li>The path from u to v traversing the fewest vertices has a score above, below, or between some threshold(s)</li>
 * </ol>
 * The scoring of a path is handled by {@link #visit(Object, Object)}, {@link #unvisit(Object, Object)},
 * {@link #isWithinRange(Object, Object)}, and the callback interface {@link EdgeWeighter}. This is most applicable to
 * {@link UndirectedGraph UndirectedGraphs} but works by <em>weak clustering</em> with directed graphs—that is, can be
 * clustered together if u is reachable from v but v is not reachable from u.
 * 
 * @author dmyersturnbull
 * 
 * @param <V> The type of the vertices
 * @param <E> The type of the edges
 */
public abstract class DistanceClusterer<V, E> implements Transformer<Graph<V, E>, Set<Set<V>>> {

	private final EdgeWeighter<E> edgeWeighter;

	public DistanceClusterer(EdgeWeighter<E> edgeWeighter) {
		super();
		this.edgeWeighter = edgeWeighter;
	}

	@Override
	public final Set<Set<V>> transform(Graph<V, E> graph) {

		Set<Set<V>> clusters = new HashSet<Set<V>>();

		// need constant-time get
		HashSet<V> unvisited = new HashSet<V>(graph.getVertices());

		// a map from every vertex to the edge used to get to it
		// works because only visit a vertex once
		HashMap<V, E> edgesTaken = new HashMap<V, E>();

		while (!unvisited.isEmpty()) {

			// first get a root for the cluster
			Set<V> cluster = new HashSet<V>();
			V root = unvisited.iterator().next();
			unvisited.remove(root);
			cluster.add(root);

			Deque<V> queue = new LinkedList<V>();
			queue.add(root);

			while (!queue.isEmpty()) {

				V vertex = queue.remove();
				E edge = edgesTaken.get(vertex);
				
				// stop traversing if we're too far
				if (!isWithinRange(root, vertex)) continue;
				
				if (edge != null) visit(vertex, edge); // this is the ONLY place where we "officially" VISIT a vertex

				Collection<V> neighbors = graph.getNeighbors(vertex);
				for (V neighbor : neighbors) {
					if (unvisited.contains(neighbor)) {
						queue.add(neighbor);
						unvisited.remove(neighbor);
						cluster.add(neighbor);
						E edgeToNeighbor = graph.findEdge(vertex, neighbor);
						edgesTaken.put(neighbor, edgeToNeighbor);
					}
				}

				clusters.add(cluster);
				unvisit(vertex, edge); // this is the ONLY place where we "officially" UNVISIT a vertex

			}

		}

		return clusters;
	}

	protected final double getEdgeWeight(E edge) {
		return edgeWeighter.getWeight(edge);
	}

	protected abstract boolean isWithinRange(V root, V vertex);

	protected abstract void unvisit(V vertex, E edge);

	protected abstract void visit(V vertex, E edge);

}
