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

import org.structnetalign.InteractionEdge;

import edu.uci.ics.jung.graph.util.Pair;

/**
 * The <em>result</em> of updating the probability of an interaction according to the weights of edges in a graph.
 * @author dmyersturnbull
 */
public class InteractionUpdate {

	private Pair<Integer> ids;
	
	private Pair<String> uniProtIds;
	
	private double initialProbability;
	
	private InteractionEdge edge;
	
	private boolean removed;

	public Pair<String> getUniProtIds() {
		return uniProtIds;
	}

	public void setUniProtIds(Pair<String> uniProtIds) {
		this.uniProtIds = uniProtIds;
	}

	public double getInitialProbability() {
		return initialProbability;
	}

	public void setInitialProbability(double initialProbability) {
		this.initialProbability = initialProbability;
	}

	public InteractionEdge getEdge() {
		return edge;
	}

	public void setEdge(InteractionEdge edge) {
		this.edge = edge;
	}

	public boolean isRemoved() {
		return removed;
	}

	public void setRemoved(boolean removed) {
		this.removed = removed;
	}

	public Pair<Integer> getIds() {
		return ids;
	}

	public void setIds(Pair<Integer> ids) {
		this.ids = ids;
	}

	public InteractionUpdate(Pair<Integer> ids, Pair<String> uniProtIds, double initialProbability,
			InteractionEdge edge, boolean removed) {
		super();
		this.ids = ids;
		this.uniProtIds = uniProtIds;
		this.initialProbability = initialProbability;
		this.edge = edge;
		this.removed = removed;
	}

	public InteractionUpdate() {
		
	}
	
}
