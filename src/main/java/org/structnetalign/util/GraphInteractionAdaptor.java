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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

/**
 * An adaptor that converts between PSI-MI XML data and graph representations.
 * 
 * @author dmyersturnbull
 */
public class GraphInteractionAdaptor {

	public static final String CONFIDENCE_FULL_NAME = "probability predicted by struct-NA";

	public static final String CONFIDENCE_SHORT_LABEL = "struct-NA confidence";

	public static final double DEFAULT_PROBABILITY = 0.5;

	public static final String INITIAL_CONFIDENCE_LABEL = "struct-NA intial weighting";

	private static final Logger logger = LogManager.getLogger("org.structnetalign");

	public static void modifyProbabilites(EntrySet entrySet, UndirectedGraph<Integer, InteractionEdge> graph) {
		modifyProbabilites(entrySet, graph, CONFIDENCE_SHORT_LABEL, CONFIDENCE_FULL_NAME);
	}

	/**
	 * Modifies {@code entrySet} by giving interactions a new confidence whose value is the edge weight.
	 * 
	 * @param entrySet
	 * @param graph
	 * @param confidenceLabel
	 * @param confidenceFullName
	 */
	public static void modifyProbabilites(EntrySet entrySet, UndirectedGraph<Integer, InteractionEdge> graph,
			String confidenceLabel, String confidenceFullName) {

		logger.info("Modifying probabilities of " + graph.getEdgeCount() + " interactions in "
				+ entrySet.getEntries().size() + " entries");
		int entryIndex = 1;
		for (Entry entry : entrySet.getEntries()) {

			logger.info("Updating entry " + entryIndex);

			for (Interaction interaction : entry.getInteractions()) {

				final NavigableSet<Integer> ids = NetworkUtils.getVertexIds(interaction);
				final InteractionEdge edge = graph.findEdge(ids.first(), ids.last());

				// a confidence with this label or full name shouldn't already exist
				// if it does, it probably means we've already run before
				// so, we'll just delete it from the output
				// it can always be recovered from the input file
				// but this is critical because otherwise we'd lose our new confidence
				Confidence alreadyExists = NetworkUtils.getExistingConfidence(interaction, confidenceLabel, confidenceFullName);
				if (alreadyExists != null) {
					logger.warn("Confidence " + confidenceLabel + " already exists. Overwriting.");
					interaction.getConfidences().remove(alreadyExists);
				}

				// make a new Confidence
				Confidence confidence = NetworkUtils.makeConfidence(edge.getWeight(), confidenceLabel, confidenceFullName);
				
				interaction.getConfidences().add(confidence);
				logger.debug("Updated interaction Id#" + interaction.getId() + " with probablility " + edge.getWeight());
				
			}
			entryIndex++;
		}

	}

	public static UndirectedGraph<Integer, InteractionEdge> toGraph(EntrySet entrySet) {
		return toGraph(entrySet, INITIAL_CONFIDENCE_LABEL, DEFAULT_PROBABILITY);
	}

	/**
	 * Converts a PSI-MI XML EntrySet to an undirected graph, using the value of the confidence with short label
	 * {@code confidenceLabel} to weight edges, and using {@code defaultProbability} othewsie.
	 */
	public static UndirectedGraph<Integer, InteractionEdge> toGraph(EntrySet entrySet, String confidenceLabel,
			double defaultProbability) {

		logger.info("Converting PSI-MI to a graph using confidence short label " + confidenceLabel
				+ " and default probability " + defaultProbability);

		UndirectedGraph<Integer, InteractionEdge> graph = new UndirectedSparseGraph<Integer, InteractionEdge>();

		logger.info("Found " + entrySet.getEntries().size() + " entries");

		int entryIndex = 1;
		for (Entry entry : entrySet.getEntries()) {

			// add the vertices
			Collection<Interactor> interactors = entry.getInteractors();
			logger.info("Adding " + interactors.size() + " vertices from entry " + entryIndex);
			int vertexIndex = 0;
			for (Interactor interactor : interactors) {
				final int id = interactor.getId();
				graph.addVertex(id);
				logger.debug("Added vertex Id#" + id + " (" + vertexIndex + "/" + interactors.size() + ")");
				vertexIndex++;
			}

			// now add the edges
			logger.info("Adding " + entry.getInteractions().size() + " edges from entry " + entryIndex);
			int edgeIndex = 1;
			for (Interaction interaction : entry.getInteractions()) {

				double probability = defaultProbability;
				Collection<Confidence> confidences = interaction.getConfidences();
				if (confidenceLabel != null && confidences != null) {
					for (Confidence confidence : confidences) {
						Unit unit = confidence.getUnit();
						if (unit != null) {
							Names names = unit.getNames();
							if (names != null) {
								String name = names.getShortLabel();
								if (confidenceLabel.equals(name)) {
									probability = Double.parseDouble(confidence.getValue());
									logger.trace("Prob(interaction)=" + probability);
								}
							}
						}
					}
				}

				InteractionEdge edge = new InteractionEdge(interaction.getId(), probability);

				NavigableSet<Integer> ids = NetworkUtils.getVertexIds(interaction); // a set of size 2

				boolean added = graph.addEdge(edge, ids.first(), ids.last());
				if (added) {
					logger.debug("Added edge Id#" + interaction.getId() + " (" + edgeIndex + "/"
							+ entry.getInteractions().size() + ") as edge (" + ids.first() + "," + ids.last() + ","
							+ probability + ")");
				} else {
					logger.warn("Edge Id#" + interaction.getId() + " (" + edgeIndex + "/"
							+ entry.getInteractions().size() + ") already exists!");
				}

				edgeIndex++;
			}
			entryIndex++;
		}
		logger.info("Read graph contains " + graph.getVertexCount() + " vertices and " + graph.getEdgeCount()
				+ " edges");
		return graph;
	}

}
