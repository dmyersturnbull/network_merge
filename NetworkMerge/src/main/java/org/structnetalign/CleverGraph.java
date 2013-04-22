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
package org.structnetalign;

import java.util.Collection;

import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

public class CleverGraph {

	public Collection<String> getVertices() {
		return homology.getVertices();
	}
	public boolean addVertex(String arg0) {
		return homology.addVertex(arg0);
	}
	public boolean containsVertex(String arg0) {
		return homology.containsVertex(arg0);
	}
	public boolean removeVertex(String arg0) {
		return homology.removeVertex(arg0);
	}
	public int getVertexCount() {
		return homology.getVertexCount();
	}
	public int getEdgeCount() {
		return homology.getEdgeCount();
	}
	
	
	public boolean isHomologous(String vertex1, String vertex2) {
		return homology.isNeighbor(vertex1, vertex2);
	}
	public boolean addHomologies(Homology edge, Collection<? extends String> vertices) {
		return homology.addEdge(edge, vertices);
	}
	public boolean addEdge(Homology edge, String vertex1, String vertex2) {
		return homology.addEdge(edge, vertex1, vertex2);
	}
	public int homologyDegree(String vertex) {
		return homology.degree(vertex);
	}
	public Collection<Homology> getHomologies(String vertex) {
		return homology.getIncidentEdges(vertex);
	}
	public Collection<String> getHomologyNeighbors(String vertex) {
		return homology.getNeighbors(vertex);
	}
	public boolean removeHomology(Homology edge) {
		return homology.removeEdge(edge);
	}

	public boolean isInteracting(String vertex1, String vertex2) {
		return interaction.isNeighbor(vertex1, vertex2);
	}
	public boolean addInteraction(Interaction edge, String vertex1, String vertex2) {
		return interaction.addEdge(edge, vertex1, vertex2);
	}
	public boolean addInteractions(Interaction arg0, Collection<? extends String> arg1) {
		return interaction.addEdge(arg0, arg1);
	}
	public int interactionDegree(String vertex) {
		return interaction.degree(vertex);
	}
	public Collection<Interaction> getInteractions(String vertex) {
		return interaction.getIncidentEdges(vertex);
	}
	public Collection<String> getInteractionNeighbors(String vertex) {
		return interaction.getNeighbors(vertex);
	}
	public boolean removeInteraction(Interaction edge) {
		return interaction.removeEdge(edge);
	}

	private final UndirectedGraph<String,Interaction> interaction;

	private final UndirectedGraph<String,Homology> homology;

	public CleverGraph() {
		interaction = new UndirectedSparseGraph<String,Interaction>();
		homology = new UndirectedSparseGraph<String,Homology>();
	}
	
	public UndirectedGraph<String, Interaction> getInteraction() {
		return interaction;
	}

	public UndirectedGraph<String, Homology> getHomology() {
		return homology;
	}
	
}
