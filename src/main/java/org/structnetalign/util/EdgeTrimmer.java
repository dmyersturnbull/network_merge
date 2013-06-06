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
package org.structnetalign.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.structnetalign.Edge;

import edu.uci.ics.jung.graph.Graph;

/**
 * A simple graph transformation that removes edges with weights above and/or below a threshold.
 * 
 * @author dmyersturnbull
 * 
 * @param <V> The types of the vertices
 * @param <E> The types of the edges
 */
public class EdgeTrimmer<V, E> {

	private final EdgeWeighter<E> edgeWeighter;

	public static <E extends Edge> EdgeTrimmer<Integer, E> forWeighted() {
		EdgeWeighter<E> weighter = new EdgeWeighter<E>() {
			@Override
			public double getWeight(E e) {
				return e.getWeight();
			}
		};
		return new EdgeTrimmer<>(weighter);
	}

	public EdgeTrimmer(EdgeWeighter<E> edgeWeighter) {
		super();
		this.edgeWeighter = edgeWeighter;
	}

	/**
	 * Removes every edge with weight strictly less than {@code minimum}.
	 */
	public void trim(Graph<V, E> graph, double minimum) {
		trim(graph, minimum, Double.MAX_VALUE);
	}

	/**
	 * Removes every edge with weight strictly less than {@code minimum} or strictly greater than {@code maximum}.
	 */
	public void trim(Graph<V, E> graph, double minimum, double maximum) {
		Collection<E> edges = graph.getEdges();
		Iterator<E> iter = edges.iterator();
		List<E> edgesToRemove = new ArrayList<>();
		while (iter.hasNext()) {
			E edge = iter.next();
			double weight = edgeWeighter.getWeight(edge);
			if (weight < minimum || weight > maximum) edgesToRemove.add(edge);
		}
		for (E edge : edgesToRemove)
			graph.removeEdge(edge);
	}

}
