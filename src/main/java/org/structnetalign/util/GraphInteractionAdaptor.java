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

import java.util.Collection;
import java.util.NavigableSet;

import org.structnetalign.InteractionEdge;

import psidev.psi.mi.xml.model.Confidence;
import psidev.psi.mi.xml.model.Entry;
import psidev.psi.mi.xml.model.EntrySet;
import psidev.psi.mi.xml.model.Interaction;
import psidev.psi.mi.xml.model.Interactor;
import psidev.psi.mi.xml.model.Names;
import psidev.psi.mi.xml.model.Unit;
import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

public class GraphInteractionAdaptor {

	public static final String CONFIDENCE_FULL_NAME = "probability predicted by struct-NA";

	public static final String CONFIDENCE_SHORT_LABEL = "struct-NA confidence";
	
	public static final String INITIAL_CONFIDENCE_LABEL = "struct-NA intial weighting";
	
	public static final double DEFAULT_PROBABILITY = 0.5;

	public static void modifyProbabilites(EntrySet entrySet, UndirectedGraph<Integer, InteractionEdge> graph) {

		for (Entry entry : entrySet.getEntries()) {
			for (Interaction interaction : entry.getInteractions()) {
				final NavigableSet<Integer> ids = NetworkUtils.getVertexIds(interaction);
				final InteractionEdge edge = graph.findEdge(ids.first(), ids.last());
				final double probability = edge.getWeight();
				Confidence confidence = new Confidence();
				Unit unit = new Unit();
				Names unitNames = new Names();
				unitNames.setShortLabel(CONFIDENCE_SHORT_LABEL);
				unitNames.setFullName(CONFIDENCE_FULL_NAME);
				unit.setNames(unitNames);
				confidence.setValue(String.valueOf(probability));
				interaction.getConfidences().add(confidence);
			}
		}

	}
	
	public static UndirectedGraph<Integer, InteractionEdge> toGraph(EntrySet entrySet) {
		return toGraph(entrySet, INITIAL_CONFIDENCE_LABEL, DEFAULT_PROBABILITY);
	}

	public static UndirectedGraph<Integer, InteractionEdge> toGraph(EntrySet entrySet, String confidenceName, double defaultProbability) {

		UndirectedGraph<Integer, InteractionEdge> graph = new UndirectedSparseGraph<Integer, InteractionEdge>();

		for (Entry entry : entrySet.getEntries()) {

			// add the vertices
			Collection<Interactor> interactors = entry.getInteractors();
			for (Interactor interactor : interactors) {
				final int id = interactor.getId();
				graph.addVertex(id);
			}

			// now add the edges
			for (Interaction interaction : entry.getInteractions()) {
				
				double probability = defaultProbability;
				Collection<Confidence> confidences = interaction.getConfidences();
				if (confidenceName != null && confidences != null) {
					for (Confidence confidence : confidences) {
						Unit unit = confidence.getUnit();
						if (unit != null) {
							Names names = unit.getNames();
							if (names != null) {
								String name = names.getShortLabel();
								if (confidenceName.equals(name)) {
									probability = Double.parseDouble(confidence.getValue());
								}
							}
						}
					}
				}

				InteractionEdge edge = new InteractionEdge(interaction.getId(), probability);

				NavigableSet<Integer> ids = NetworkUtils.getVertexIds(interaction); // a set of size 2

				graph.addEdge(edge, ids.first(), ids.last());

			}

		}

		return graph;
	}

}
