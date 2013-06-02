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
import java.util.HashSet;
import java.util.Set;

import org.structnetalign.HomologyEdge;
import org.structnetalign.InteractionEdge;

public class MergeUpdate {

	private int v0;
	
	private Set<Integer> vertices;

	private Set<HomologyEdge> homologyEdges;

	private Set<InteractionEdge> interactionEdges;

	public MergeUpdate(int v0) {
		super();
		this.v0 = v0;
		this.vertices = new HashSet<>();
		this.interactionEdges = new HashSet<>();
		this.homologyEdges = new HashSet<>();
	}

	public int getV0() {
		return v0;
	}

	public Set<Integer> getVertices() {
		return vertices;
	}

	public Set<InteractionEdge> getInteractionEdges() {
		return interactionEdges;
	}

	public boolean addInteraction(InteractionEdge e) {
		return interactionEdges.add(e);
	}

	public boolean addInteraction(HomologyEdge e) {
		return homologyEdges.add(e);
	}

	public boolean addVertex(int v) {
		return vertices.add(v);
	}
	
	public Set<HomologyEdge> getHomologyEdges() {
		return homologyEdges;
	}

	public void add(int vertex, Collection<InteractionEdge> interactions, Collection<HomologyEdge> homologies) {
		vertices.add(vertex);
		interactionEdges.addAll(interactions);
		homologyEdges.addAll(homologies);
	}
	
	public void combine(MergeUpdate update) {
		add(update.v0, update.interactionEdges, update.homologyEdges);
	}
	
}
