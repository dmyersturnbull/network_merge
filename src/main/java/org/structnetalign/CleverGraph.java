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
package org.structnetalign;

import java.util.Collection;

import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.util.Pair;

public class CleverGraph {

	private final UndirectedGraph<Integer, HomologyEdge> homology;

	private final UndirectedGraph<Integer, InteractionEdge> interaction;

	public CleverGraph() {
		interaction = new UndirectedSparseGraph<Integer, InteractionEdge>();
		homology = new UndirectedSparseGraph<Integer, HomologyEdge>();
	}

	public CleverGraph(UndirectedGraph<Integer, InteractionEdge> interaction) {
		this.interaction = interaction;
		homology = new UndirectedSparseGraph<Integer, HomologyEdge>();
		for (int vertex : interaction.getVertices()) {
			homology.addVertex(vertex);
		}
	}

	public CleverGraph(UndirectedGraph<Integer, InteractionEdge> interaction,
			UndirectedGraph<Integer, HomologyEdge> homology) {
		this.interaction = interaction;
		this.homology = homology;
	}

	public boolean addHomologies(HomologyEdge edge, Collection<? extends Integer> vertices) {
		return homology.addEdge(edge, vertices);
	}

	public boolean addHomology(HomologyEdge edge, int vertex1, int vertex2) {
		return homology.addEdge(edge, vertex1, vertex2);
	}

	public boolean addInteraction(InteractionEdge edge, int vertex1, int vertex2) {
		return interaction.addEdge(edge, vertex1, vertex2);
	}

	public boolean addInteractions(InteractionEdge arg0, Collection<? extends Integer> arg1) {
		return interaction.addEdge(arg0, arg1);
	}

	public boolean addVertex(int vertex) {
		return interaction.addVertex(vertex) & homology.addVertex(vertex); // single ampersand
	}

	public UndirectedGraph<Integer, Edge> buildCombinedGraph() {

		UndirectedGraph<Integer, Edge> combined = new UndirectedSparseGraph<>();
		for (int vertex : getVertices()) {
			combined.addVertex(vertex);
		}

		for (HomologyEdge edge : getHomologies()) {
			Pair<Integer> vertices = getHomology().getEndpoints(edge);
			combined.addEdge(edge, vertices);
		}

		for (InteractionEdge edge : getInteractions()) {
			Pair<Integer> vertices = getInteraction().getEndpoints(edge);
			combined.addEdge(edge, vertices);
		}

		return combined;

	}

	public boolean containsVertex(int vertex) {
		return homology.containsVertex(vertex);
	}

	public Collection<HomologyEdge> getHomologies() {
		return homology.getEdges();
	}

	public Collection<HomologyEdge> getHomologies(int vertex) {
		return homology.getIncidentEdges(vertex);
	}

	public UndirectedGraph<Integer, HomologyEdge> getHomology() {
		return homology;
	}

	public int getHomologyCount() {
		return homology.getEdgeCount();
	}

	public Collection<Integer> getHomologyNeighbors(int vertex) {
		return homology.getNeighbors(vertex);
	}

	public UndirectedGraph<Integer, InteractionEdge> getInteraction() {
		return interaction;
	}

	public int getInteractionCount() {
		return interaction.getEdgeCount();
	}

	public Collection<Integer> getInteractionNeighbors(int vertex) {
		return interaction.getNeighbors(vertex);
	}

	public Collection<InteractionEdge> getInteractions() {
		return interaction.getEdges();
	}

	public Collection<InteractionEdge> getInteractions(int vertex) {
		return interaction.getIncidentEdges(vertex);
	}

	public int getVertexCount() {
		return homology.getVertexCount();
	}

	public Collection<Integer> getVertices() {
		return homology.getVertices();
	}

	public int homologyDegree(int vertex) {
		return homology.degree(vertex);
	}

	public int interactionDegree(int vertex) {
		return interaction.degree(vertex);
	}

	public boolean isHomologous(int vertex1, int vertex2) {
		return homology.isNeighbor(vertex1, vertex2);
	}

	public boolean isInteracting(int vertex1, int vertex2) {
		return interaction.isNeighbor(vertex1, vertex2);
	}

	public boolean removeHomology(HomologyEdge edge) {
		return homology.removeEdge(edge);
	}

	public boolean removeInteraction(InteractionEdge edge) {
		return interaction.removeEdge(edge);
	}

	public boolean removeVertex(int vertex) {
		return homology.removeVertex(vertex) & interaction.removeVertex(vertex); // SINGLE ampersand!
	}

}
