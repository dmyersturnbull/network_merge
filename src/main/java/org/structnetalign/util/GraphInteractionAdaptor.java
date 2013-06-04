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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.structnetalign.InteractionEdge;
import org.structnetalign.PipelineProperties;

import psidev.psi.mi.xml.model.Attribute;
import psidev.psi.mi.xml.model.Confidence;
import psidev.psi.mi.xml.model.Entry;
import psidev.psi.mi.xml.model.EntrySet;
import psidev.psi.mi.xml.model.Interaction;
import psidev.psi.mi.xml.model.Interactor;
import psidev.psi.mi.xml.model.PsiFactory;
import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.util.Pair;

/**
 * An adaptor that converts between PSI-MI XML data and graph representations.
 * 
 * @author dmyersturnbull
 */
public class GraphInteractionAdaptor {

	private static final Logger logger = LogManager.getLogger("org.structnetalign");

	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			System.err.println("Usage: " + GraphInteractionAdaptor.class.getSimpleName()
					+ " input-psimi25-file output-graphml-file");
			return;
		}
		EntrySet entrySet = NetworkUtils.readNetwork(args[0]);
		UndirectedGraph<Integer, InteractionEdge> graph = GraphInteractionAdaptor.toGraph(entrySet);
		GraphMLAdaptor.writeInteractionGraph(graph, new File(args[1]));
	}

	public static List<InteractionUpdate> modifyProbabilites(EntrySet entrySet, UndirectedGraph<Integer, InteractionEdge> graph, Map<Integer,Integer> ER) {
		return modifyProbabilites(entrySet, graph, PipelineProperties.getInstance().getOutputConfLabel(), PipelineProperties.getInstance().getOutputConfName(), ER);
	}

	/**
	 * Modifies {@code entrySet} by giving interactions a new confidence whose value is the edge weight.
	 * 
	 * @param entrySet
	 * @param graph
	 * @param confidenceLabel
	 * @param confidenceFullName
	 * @param A map of non-representative interaction Ids to their representative interaction Ids ER; can be null
	 */
	public static List<InteractionUpdate> modifyProbabilites(EntrySet entrySet, UndirectedGraph<Integer, InteractionEdge> graph,
			String confidenceLabel, String confidenceFullName, Map<Integer,Integer> representativeIds) {

		List<InteractionUpdate> updates = new ArrayList<>();

		logger.info("Modifying probabilities of " + graph.getEdgeCount() + " interactions in "
				+ entrySet.getEntries().size() + " entries");
		int entryIndex = 1;
		for (Entry entry : entrySet.getEntries()) {

			logger.info("Updating entry " + entryIndex);

			for (Interaction interaction : entry.getInteractions()) {

				final NavigableSet<Integer> ids = NetworkUtils.getVertexIds(interaction);
				final Pair<Integer> idsPair = new Pair<>(ids.first(), ids.last());
				InteractionEdge edge = graph.findEdge(ids.first(), ids.last());
				Pair<String> uniProtIds = NetworkUtils.getUniProtId(interaction);

				// we're also interested in the initial confidence so we can report it
				Confidence initialConf = NetworkUtils.getExistingConfidence(interaction, PipelineProperties.getInstance().getInitialConfLabel(), PipelineProperties.getInstance().getInitialConfName());
				Double initialProb = null;
				if (initialConf != null) {
					initialProb = Double.parseDouble(initialConf.getValue());
				}
				
				// this is only true if we edge-contracted that vertex
				if (edge == null) {
					logger.debug("No edge for " + interaction.getId());
					Pair<Interactor> interactors = NetworkUtils.getInteractors(interaction);
					// create a new Attribute stating this has been removed, and given it the Id of V0
					String v0 = "unknown"; // here's the Id
					if (representativeIds != null) {
						v0 = String.valueOf(representativeIds.get(interaction.getId()));
					}
					Attribute removal = PsiFactory.createAttribute(PipelineProperties.getInstance().getRemovedAttributeLabel(), v0);
					interaction.getAttributes().add(removal); // add to interaction
					interactors.getFirst().getAttributes().add(removal); // add to interactor0
					interactors.getSecond().getAttributes().add(removal); // add to interactor1
					InteractionUpdate update = new InteractionUpdate(idsPair, uniProtIds, initialProb, edge, true); // report the deletion
					updates.add(update);
					continue;
				}
				
				// don't update if we don't need to
				// this skipping is mostly for reporting
				if (initialProb != null) {
					if (initialProb == edge.getWeight()) {
						logger.trace("Skipping " + edge.getId() + " as unmodified");
						continue;
					}
				}

				// a confidence with this label or full name shouldn't already exist
				// if it does, it probably means we've already run before
				// so, we'll just delete it from the output
				// it can always be recovered from the input file
				// but this is critical because otherwise we'd lose our new confidence
				Confidence alreadyExists = NetworkUtils.getExistingConfidence(interaction, confidenceLabel,
						confidenceFullName);
				if (alreadyExists != null) {
					logger.warn("Confidence " + confidenceLabel + " already exists. Overwriting.");
					interaction.getConfidences().remove(alreadyExists);
				}

				// report the update
				InteractionUpdate update = new InteractionUpdate(idsPair, uniProtIds, initialProb, edge, false);
				updates.add(update);

				// make a new Confidence
				Confidence confidence = NetworkUtils.makeConfidence(edge.getWeight(), confidenceLabel,
						confidenceFullName, confidenceLabel);

				interaction.getConfidences().add(confidence);
				logger.debug("Updated interaction Id#" + interaction.getId() + " with probablility " + PipelineProperties.getInstance().getDisplayFormatter().format(edge.getWeight()));

			}
			entryIndex++;
		}

		return updates;

	}

	public static UndirectedGraph<Integer, InteractionEdge> toGraph(EntrySet entrySet) {
		return toGraph(entrySet, PipelineProperties.getInstance().getInitialConfLabel(), PipelineProperties.getInstance().getInitialConfName());
	}

	/**
	 * Converts a PSI-MI XML EntrySet to an undirected graph, using the value of the confidence with short label
	 * {@code confidenceLabel} to weight edges, and using {@code defaultProbability} othewsie.
	 */
	public static UndirectedGraph<Integer, InteractionEdge> toGraph(EntrySet entrySet, String confidenceLabel, String confidenceName) {

		logger.info("Converting PSI-MI to a graph using confidence short label " + confidenceLabel);

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

				// note that using confidenceLabel twice here is weird
				double probability = -1;
				if (confidenceLabel == null && confidenceName == null) {
					probability = 1.0; // we're probably just testing (or otherwise don't need real probabilities
				} else {
					Confidence conf = NetworkUtils.getExistingConfidence(interaction, confidenceLabel, confidenceName);
					if (conf == null) {
						throw new IllegalArgumentException("Initial confidence " + confidenceLabel + " missing for " + interaction.getId());
					}
					probability = Double.parseDouble(conf.getValue());
				}
				logger.trace("Prob(interaction)=" + probability);

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
