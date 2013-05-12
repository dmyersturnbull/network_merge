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

import org.structnetalign.util.EdgeWeighter;

/**
 * A {@link DistanceClusterer} that clusters vertices by reachability and distance, where edge weights denote
 * probabilities and the probability of the shortest (fewst-vertex) path is required to be above a threshold.
 * 
 * @author dmyersturnbull
 * 
 * @param <V>
 * @param <E>
 */
public class ProbabilisticDistanceClusterer<V, E> extends DistanceClusterer<V, E> {

	/**
	 * The log of the probability that we don't take a path from root to a vertex.
	 */
	private transient double logProbability = 0;

	private final double minProbability;

	public ProbabilisticDistanceClusterer(EdgeWeighter<E> edgeWeighter, double minProbability) {
		super(edgeWeighter);
		this.minProbability = minProbability;
	}

	@Override
	protected boolean isWithinRange(V root, V vertex) {
		return 1 - Math.exp(logProbability) >= minProbability;
	}

	@Override
	protected void unvisit(V vertex, E edge) {
		logProbability -= Math.log(1 - getEdgeWeight(edge));
	}

	@Override
	protected void visit(V vertex, E edge) {
		logProbability += Math.log(1 - getEdgeWeight(edge));
	}

}
