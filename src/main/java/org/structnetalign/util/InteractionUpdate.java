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

public class InteractionUpdate {

	private Pair<String> uniProtIds;
	
	private double initialProbability;
	
	private InteractionEdge edge;

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

	public InteractionUpdate(Pair<String> uniProtIds, double initialProbability, InteractionEdge edge) {
		super();
		this.uniProtIds = uniProtIds;
		this.initialProbability = initialProbability;
		this.edge = edge;
	}
	
	public InteractionUpdate() {
		
	}
	
}
